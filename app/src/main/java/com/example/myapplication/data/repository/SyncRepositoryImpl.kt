package com.example.myapplication.data.repository

import com.example.myapplication.BuildConfig
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.core.preferences.AuthPreferences
import com.example.myapplication.data.local.dao.AttendanceDao
import com.example.myapplication.data.local.dao.CatalogDao
import com.example.myapplication.data.local.dao.GradeDao
import com.example.myapplication.data.local.dao.IncidentDao
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.local.entity.StudentCourseEntity
import com.example.myapplication.data.mapper.toSyncBundle
import com.example.myapplication.data.mapper.mapSeverityRemoteToLocalName
import com.example.myapplication.data.mapper.toAsistenciaInsertDto
import com.example.myapplication.data.mapper.toCalificacionInsertDto
import com.example.myapplication.data.mapper.toEpochMillisOrNull
import com.example.myapplication.data.mapper.toIncidentEntity
import com.example.myapplication.data.mapper.toIncidenteInsertDto
import com.example.myapplication.data.mapper.toStudentEntity
import com.example.myapplication.data.remote.api.SupabaseApiService
import com.example.myapplication.domain.model.sync.SyncOutcome
import com.example.myapplication.domain.repository.CatalogRepository
import com.example.myapplication.domain.repository.SyncRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Implementación de sincronización Offline First.
 *
 * Reglas por entidad:
 *   - `estudiantes`         → PULL (catálogo). Sin push.
 *   - `asistencias`         → PUSH local→remoto vía upsert por `uuid`.
 *   - `calificaciones`      → PUSH local→remoto vía upsert por `uuid`.
 *   - `incidentes`          → **PULL + PUSH** (Fase 14 revirtió la decisión
 *                             original de PULL only). Upsert idempotente por
 *                             `uuid`. Excluye los que tienen `studentId<=0`
 *                             (estudiante local sin sincronizar).
 *
 * **Telegram** (Fase 13) corre fuera de esta clase, en `SendPendingIncidentsUseCase`
 * disparado desde `SyncWorker`. Es ortogonal a la sincronización con Supabase:
 * cada canal tiene su propia columna de estado (`sync_status` para Supabase,
 * `enviado` para Telegram) y reintenta independientemente.
 *
 * Resolución de conflictos (Fase 8):
 *   - LWW por `server_updated_at` (sólo para datos que vienen del servidor).
 *   - Local PENDING (sync_status IDLE/ERROR) gana frente a remoto.
 *   - Incidentes traídos del PULL no se sobrescriben con datos locales
 *     diferentes (UUID distinto → entidades distintas).
 */
class SyncRepositoryImpl(
    private val supabaseApi: SupabaseApiService?,
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao,
    private val gradeDao: GradeDao,
    private val incidentDao: IncidentDao,
    private val catalogDao: CatalogDao,
    private val catalogRepository: CatalogRepository,
    private val networkMonitor: NetworkMonitor,
    private val authPreferences: AuthPreferences,
    private val teacherDataSyncer: TeacherDataSyncer
) : SyncRepository {

    override suspend fun syncAll(): SyncOutcome {
        if (!networkMonitor.currentlyOnline()) {
            return SyncOutcome.Skipped("Sin conexión. Se usan datos locales.")
        }
        if (supabaseApi == null) {
            return SyncOutcome.Skipped(
                "Supabase no configurado. Revisa SUPABASE_URL y SUPABASE_KEY en local.properties."
            )
        }

        val catalogsOutcome = catalogRepository.syncCatalogsFromRemote()
        val studentsOutcome = syncStudentsFromRemote()
        val incidentsOutcome = pullIncidentsFromRemote()
        val pendingOutcome = syncPendingRecords()

        val failures = listOf(catalogsOutcome, studentsOutcome, incidentsOutcome, pendingOutcome)
            .filterIsInstance<SyncOutcome.Failure>()
        if (failures.isNotEmpty()) {
            return SyncOutcome.Failure(failures.joinToString(" | ") { it.message })
        }

        val parts = listOfNotNull(
            (catalogsOutcome as? SyncOutcome.Success)?.message,
            (studentsOutcome as? SyncOutcome.Success)?.message,
            (incidentsOutcome as? SyncOutcome.Success)?.message,
            (pendingOutcome as? SyncOutcome.Success)?.message
        )
        return SyncOutcome.Success(parts.joinToString(" "))
    }

    // ---------- ESTUDIANTES (PULL) ----------

    override suspend fun syncStudentsFromRemote(): SyncOutcome {
        if (!networkMonitor.currentlyOnline()) {
            return SyncOutcome.Skipped("Sin conexión para sincronizar estudiantes.")
        }
        val api = supabaseApi ?: return SyncOutcome.Skipped("Supabase no configurado.")
        val teacherId = authPreferences.session.firstOrNull()?.userId
        if (teacherId != null) {
            return teacherDataSyncer.syncForTeacher(teacherId)
        }

        return runCatching {
            val remoteStudents = api.getEstudiantesActivos()
            if (remoteStudents.isEmpty()) {
                return SyncOutcome.Skipped("No hay estudiantes activos en el servidor.")
            }
            val bundles = remoteStudents.map { it.toSyncBundle() }
            studentDao.insertOrReplaceStudents(bundles.map { it.student })

            // Matrículas desde tabla estudiante_curso (más fiable que el embed anidado).
            val studentIds = bundles.map { it.student.id }.toSet()
            val fromTable = api.getEstudianteCursosActivos()
                .filter { it.estudianteId in studentIds && it.cursoId > 0L }
                .map { row ->
                    StudentCourseEntity(studentId = row.estudianteId, courseId = row.cursoId)
                }
            val fromEmbed = bundles.flatMap { it.courseLinks }
            val courseLinks = if (fromTable.isNotEmpty()) {
                fromTable
            } else {
                fromEmbed
            }.distinctBy { "${it.studentId}_${it.courseId}" }

            catalogDao.clearStudentRepresentatives()
            if (courseLinks.isNotEmpty()) {
                catalogDao.clearStudentCourses()
                catalogDao.replaceStudentCourses(courseLinks)
            }
            catalogDao.replaceStudentRepresentatives(bundles.flatMap { it.representatives })
            val extraTelegram = bundles.flatMap { it.telegramConfigs }
            if (extraTelegram.isNotEmpty()) {
                catalogDao.replaceTelegramConfigs(extraTelegram)
            }
            val linkSource = when {
                fromTable.isNotEmpty() -> "tabla estudiante_curso"
                fromEmbed.isNotEmpty() -> "embed estudiantes"
                else -> "sin matrículas (revisa GRANT/RLS en estudiante_curso)"
            }
            SyncOutcome.Success(
                "${bundles.size} estudiantes, ${courseLinks.size} matrículas curso ($linkSource)."
            )
        }.getOrElse { error ->
            SyncOutcome.Failure(formatSyncError(error, "Error al traer estudiantes desde Supabase."))
        }
    }

    // ---------- INCIDENTES (PULL ONLY) ----------

    /**
     * PULL only — mobile NUNCA escribe incidentes en Supabase.
     *
     * Conflictos: si el incidente ya existe localmente con el mismo `uuid`,
     * se actualiza solo si el `server_updated_at` remoto es más reciente
     * (LWW para datos del servidor; incidentes locales con `localOnly=true`
     * no se tocan jamás porque tienen distinto `uuid`).
     */
    private suspend fun pullIncidentsFromRemote(): SyncOutcome {
        if (!networkMonitor.currentlyOnline()) {
            return SyncOutcome.Skipped("Sin conexión para PULL de incidentes.")
        }
        val api = supabaseApi ?: return SyncOutcome.Skipped("Supabase no configurado.")

        return runCatching {
            val remote = api.getIncidentes()
            if (remote.isEmpty()) {
                return SyncOutcome.Success("0 incidentes nuevos en servidor.")
            }
            var inserted = 0
            var updated = 0
            var skipped = 0
            remote.forEach { dto ->
                val incomingTs = dto.updatedAt?.toEpochMillisOrNull() ?: 0L
                val existing = dto.uuid?.let { incidentDao.getByUuid(it) }
                val entity = dto.toIncidentEntity(
                    typeToString = ::mapTipoIncidenteRemote,
                    severityRemoteToLocal = ::mapSeverityRemoteToLocalName
                )
                when {
                    existing == null -> {
                        incidentDao.insertOrReplaceIncident(entity)
                        inserted++
                    }
                    // No sobrescribir incidentes creados localmente
                    existing.localOnly -> skipped++
                    // LWW: solo actualizar si el servidor es más reciente
                    (existing.serverUpdatedAt ?: 0L) < incomingTs -> {
                        incidentDao.insertOrReplaceIncident(entity.copy(id = existing.id))
                        updated++
                    }
                    else -> skipped++
                }
            }
            SyncOutcome.Success(
                "Incidentes: $inserted nuevos, $updated actualizados, $skipped sin cambios."
            )
        }.getOrElse { error ->
            SyncOutcome.Failure(formatSyncError(error, "Error al hacer PULL de incidentes."))
        }
    }

    /**
     * Convierte `tipo_incidente_id` (FK a `tipos_incidente`) a String del enum
     * local [IncidentType]. Aproximación por defaults conocidos.
     *
     * Si llega un id desconocido, se devuelve `OTHER` para no perder el incidente.
     * Mapeo robusto se hará con tabla `tipos_incidente` cuando se cachee.
     */
    /** Guarda el id remoto como string para alinear con catálogo `tipos_incidente`. */
    private fun mapTipoIncidenteRemote(tipoIncidenteId: Long): String = tipoIncidenteId.toString()

    // ---------- ASISTENCIAS + CALIFICACIONES (PUSH) ----------

    override suspend fun syncPendingRecords(): SyncOutcome {
        if (!networkMonitor.currentlyOnline()) {
            return SyncOutcome.Skipped("Sin conexión para subir pendientes.")
        }
        val api = supabaseApi ?: return SyncOutcome.Skipped("Supabase no configurado.")

        return runCatching {
            val now = System.currentTimeMillis()
            var uploaded = 0
            var failed = 0

            // Asistencias
            val pendingAttendance = attendanceDao.getPendingSync()
            pendingAttendance.forEach { entity ->
                attendanceDao.markAsSending(entity.uuid, now)
                runCatching {
                    val response = api.upsertAsistencia(
                        body = entity.toAsistenciaInsertDto(
                            defaultCourseId = BuildConfig.SUPABASE_DEFAULT_CURSO_ID,
                            defaultMateriaId = BuildConfig.SUPABASE_DEFAULT_MATERIA_ID
                        )
                    ).firstOrNull()
                    attendanceDao.markAsSynced(
                        uuid = entity.uuid,
                        remoteId = response?.id,
                        serverTs = response?.updatedAt?.toEpochMillisOrNull()
                    )
                    uploaded++
                }.onFailure { error ->
                    attendanceDao.markAsFailed(entity.uuid, error.message.orEmpty(), now)
                    failed++
                }
            }

            // Calificaciones
            val pendingGrades = gradeDao.getPendingSync()
            pendingGrades.forEach { entity ->
                gradeDao.markAsSending(entity.uuid, now)
                runCatching {
                    val response = api.upsertCalificacion(
                        body = entity.toCalificacionInsertDto(
                            defaultCourseId = BuildConfig.SUPABASE_DEFAULT_CURSO_ID,
                            defaultMateriaId = BuildConfig.SUPABASE_DEFAULT_MATERIA_ID,
                            defaultTipoEvaluacionId = BuildConfig.SUPABASE_DEFAULT_TIPO_EVALUACION_ID,
                            defaultPeriodoId = BuildConfig.SUPABASE_DEFAULT_PERIODO_ID.takeIf { it > 0L }
                        )
                    ).firstOrNull()
                    gradeDao.markAsSynced(
                        uuid = entity.uuid,
                        remoteId = response?.id,
                        serverTs = response?.updatedAt?.toEpochMillisOrNull()
                    )
                    uploaded++
                }.onFailure { error ->
                    gradeDao.markAsFailed(entity.uuid, error.message.orEmpty(), now)
                    failed++
                }
            }

            // Incidentes (Fase 14 — el equipo revirtió la decisión PULL-only).
            // Solo se envían los que tienen estudiante remoto válido (id positivo).
            // Si el incidente referencia un estudiante creado offline (id < 0),
            // se marca como ERROR explicando la razón hasta que se resuelva.
            val pendingIncidents = incidentDao.getPendingPushToSupabase()
            pendingIncidents.forEach { entity ->
                if (entity.studentId <= 0L) {
                    incidentDao.markPushFailed(
                        uuid = entity.uuid,
                        error = "Estudiante local (id ${entity.studentId}) sin " +
                            "sincronizar a Supabase. Asocia el incidente a un " +
                            "estudiante del listado oficial.",
                        now = now
                    )
                    failed++
                    return@forEach
                }
                incidentDao.markPushSending(entity.uuid, now)
                runCatching {
                    val response = api.upsertIncidente(
                        body = entity.toIncidenteInsertDto(
                            defaultTipoIncidenteId = BuildConfig.SUPABASE_DEFAULT_TIPO_INCIDENTE_ID,
                            defaultReportadoPorId = BuildConfig.SUPABASE_DEFAULT_REPORTADO_POR_ID
                                .takeIf { it > 0L }
                        )
                    ).firstOrNull()
                    incidentDao.markPushSynced(
                        uuid = entity.uuid,
                        remoteId = response?.id,
                        serverTs = response?.updatedAt?.toEpochMillisOrNull()
                    )
                    uploaded++
                }.onFailure { error ->
                    incidentDao.markPushFailed(entity.uuid, error.message.orEmpty(), now)
                    failed++
                }
            }

            buildOutcome(uploaded, failed)
        }.getOrElse { error ->
            SyncOutcome.Failure(formatSyncError(error, "Error al subir registros pendientes."))
        }
    }

    private fun buildOutcome(uploaded: Int, failed: Int): SyncOutcome = when {
        uploaded == 0 && failed == 0 -> SyncOutcome.Success("No hay registros pendientes.")
        failed > 0 -> SyncOutcome.Success(
            "$uploaded enviado(s), $failed con error (se reintentarán)."
        )
        else -> SyncOutcome.Success("$uploaded registro(s) enviado(s) a Supabase.")
    }

    private fun formatSyncError(error: Throwable, fallback: String): String {
        val raw = error.message.orEmpty()
        if (raw.contains("HTTP 400", ignoreCase = true) ||
            raw.contains("42703", ignoreCase = true) ||
            raw.contains("does not exist", ignoreCase = true)
        ) {
            return "Supabase rechazó la consulta (400). Actualiza la app o revisa columnas del esquema. Detalle: $raw"
        }
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
