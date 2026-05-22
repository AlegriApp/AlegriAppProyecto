package com.example.myapplication.data.repository

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.local.dao.AttendanceDao
import com.example.myapplication.data.local.dao.GradeDao
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.mapper.toAsistenciaInsertDto
import com.example.myapplication.data.mapper.toCalificacionInsertDto
import com.example.myapplication.data.mapper.toStudentEntity
import com.example.myapplication.data.remote.api.SupabaseApiService
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.domain.model.sync.SyncOutcome
import com.example.myapplication.domain.repository.SyncRepository

class SyncRepositoryImpl(
    private val supabaseApi: SupabaseApiService?,
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao,
    private val gradeDao: GradeDao,
    private val networkMonitor: NetworkMonitor
) : SyncRepository {

    override suspend fun syncAll(): SyncOutcome {
        if (!networkMonitor.currentlyOnline()) {
            return SyncOutcome.Skipped("Sin conexión. Se usan datos locales.")
        }
        if (supabaseApi == null) {
            return SyncOutcome.Skipped("Supabase no configurado. Revisa SUPABASE_URL y SUPABASE_KEY en local.properties.")
        }

        val studentsOutcome = syncStudentsFromRemote()
        val pendingOutcome = syncPendingRecords()

        return when {
            studentsOutcome is SyncOutcome.Failure -> studentsOutcome
            pendingOutcome is SyncOutcome.Failure -> pendingOutcome
            else -> SyncOutcome.Success(
                buildString {
                    append((studentsOutcome as? SyncOutcome.Success)?.message ?: "Estudiantes en cache local.")
                    append(" ")
                    append((pendingOutcome as? SyncOutcome.Success)?.message ?: "")
                }.trim()
            )
        }
    }

    override suspend fun syncStudentsFromRemote(): SyncOutcome {
        if (!networkMonitor.currentlyOnline()) {
            return SyncOutcome.Skipped("Sin conexión para sincronizar estudiantes.")
        }
        val api = supabaseApi ?: return SyncOutcome.Skipped("Supabase no configurado.")

        return runCatching {
            val remoteStudents = api.getEstudiantesActivos()
            if (remoteStudents.isEmpty()) {
                return SyncOutcome.Skipped("No hay estudiantes activos en el servidor.")
            }
            val entities = remoteStudents.map { it.toStudentEntity() }
            studentDao.insertOrReplaceStudents(entities)
            SyncOutcome.Success("${entities.size} estudiantes sincronizados desde Supabase.")
        }.getOrElse { error ->
            SyncOutcome.Failure(formatSyncError(error, "Error al traer estudiantes desde Supabase."))
        }
    }

    override suspend fun syncPendingRecords(): SyncOutcome {
        if (!networkMonitor.currentlyOnline()) {
            return SyncOutcome.Skipped("Sin conexión para subir pendientes.")
        }
        val api = supabaseApi ?: return SyncOutcome.Skipped("Supabase no configurado.")

        return runCatching {
            var uploaded = 0

            val pendingAttendance = attendanceDao.getPendingSyncAttendance()
            pendingAttendance.forEach { entity ->
                api.insertAsistencia(entity.toAsistenciaInsertDto(BuildConfig.SUPABASE_DEFAULT_CURSO_ID))
                uploaded++
            }
            if (pendingAttendance.isNotEmpty()) {
                attendanceDao.markAsSynced(pendingAttendance.map { it.id })
            }

            val pendingGrades = gradeDao.getPendingSyncGrades()
            pendingGrades.forEach { entity ->
                api.insertCalificacion(
                    entity.toCalificacionInsertDto(
                        defaultCourseId = BuildConfig.SUPABASE_DEFAULT_CURSO_ID,
                        defaultMateriaId = BuildConfig.SUPABASE_DEFAULT_MATERIA_ID,
                        defaultTipoEvaluacionId = BuildConfig.SUPABASE_DEFAULT_TIPO_EVALUACION_ID,
                        defaultPeriodoId = BuildConfig.SUPABASE_DEFAULT_PERIODO_ID.takeIf { it > 0L }
                    )
                )
                uploaded++
            }
            if (pendingGrades.isNotEmpty()) {
                gradeDao.markAsSynced(pendingGrades.map { it.id })
            }

            if (uploaded == 0) {
                SyncOutcome.Success("No hay registros pendientes de sincronizar.")
            } else {
                SyncOutcome.Success("$uploaded registro(s) enviado(s) a Supabase.")
            }
        }.getOrElse { error ->
            SyncOutcome.Failure(formatSyncError(error, "Error al subir registros pendientes."))
        }
    }

    private fun formatSyncError(error: Throwable, fallback: String): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("row-level security", ignoreCase = true) ||
                raw.contains("42501") ->
                "Supabase bloqueó el INSERT (RLS). Ejecuta supabase_fix_insert_rls.sql en SQL Editor."
            raw.contains("tipos_evaluacion", ignoreCase = true) ||
                raw.contains("23503") ->
                "IDs inválidos en Supabase. Revisa SUPABASE_DEFAULT_TIPO_EVALUACION_ID y PERIODO_ID en local.properties."
            raw.contains("409") || raw.contains("duplicate", ignoreCase = true) ->
                "Ya existe ese registro en Supabase (conflicto 409)."
            else -> raw.ifBlank { fallback }
        }
    }
}
