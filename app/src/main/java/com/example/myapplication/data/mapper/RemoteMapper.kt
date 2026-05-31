package com.example.myapplication.data.mapper

import com.example.myapplication.core.common.newUuid
import com.example.myapplication.data.local.entity.AttendanceEntity
import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.data.local.entity.IncidentEntity
import com.example.myapplication.data.local.entity.StudentEntity
import com.example.myapplication.data.remote.dto.AsistenciaInsertDto
import com.example.myapplication.data.remote.dto.CalificacionInsertDto
import com.example.myapplication.data.remote.dto.EstudianteRemoteDto
import com.example.myapplication.data.remote.dto.IncidenteRemoteDto
import com.example.myapplication.domain.model.AttendanceStatus
import com.example.myapplication.domain.model.sync.SyncState
import java.time.Instant

// ---------------------------------------------------------------------------
// PULL: Supabase → Room
// ---------------------------------------------------------------------------

fun EstudianteRemoteDto.toStudentEntity(): StudentEntity {
    val activeEnrollment = estudianteCurso
        ?.firstOrNull { it.estado == "activo" }
        ?: estudianteCurso?.firstOrNull()

    val nivel = activeEnrollment?.cursos?.nivelAcademico?.nombre?.trim().orEmpty()
    val paralelo = activeEnrollment?.cursos?.paralelo?.trim().orEmpty()

    return StudentEntity(
        id = id,
        fullName = listOf(nombre, apellido).joinToString(" ").trim(),
        grade = nivel.ifBlank { "Sin nivel" },
        section = paralelo.ifBlank { "?" },
        representativeName = "",
        telegramChatId = null,
        uuid = uuid ?: newUuid(),
        remoteId = id,
        syncStatus = SyncState.Stored.SUCCESS,
        serverUpdatedAt = updatedAt?.toEpochMillisOrNull(),
        isDeleted = deletedAt != null
    )
}

/**
 * Convierte un incidente remoto a entidad local. **PULL only.**
 * Marca `localOnly = false` para excluirlo de cualquier intento de push futuro.
 */
fun IncidenteRemoteDto.toIncidentEntity(
    typeToString: (Long) -> String = { it.toString() },
    severityRemoteToLocal: (String) -> String = ::mapSeverityRemoteToLocalName
): IncidentEntity = IncidentEntity(
    id = 0L,
    studentId = estudianteId,
    type = typeToString(tipoIncidenteId),
    severity = severityRemoteToLocal(nivelGravedad),
    description = descripcion,
    dateTime = fechaHora,
    teacherName = null,
    sent = true,
    syncPending = false,
    uuid = uuid ?: newUuid(),
    remoteId = id,
    syncStatus = SyncState.Stored.SUCCESS,
    syncError = null,
    lastSyncAttempt = null,
    serverUpdatedAt = updatedAt?.toEpochMillisOrNull(),
    isDeleted = deletedAt != null,
    localOnly = false
)

/**
 * Postgres CHECK: `bajo|medio|alto|critico`.
 * Enum mobile actual sólo tiene `LOW|MEDIUM|HIGH` (sin CRITICAL).
 * Mapeamos `crítico` remoto → `HIGH` local para no perder información.
 */
fun mapSeverityRemoteToLocalName(remote: String): String = when (remote.lowercase()) {
    "bajo" -> "LOW"
    "medio" -> "MEDIUM"
    "alto" -> "HIGH"
    "critico", "crítico" -> "HIGH"
    else -> "MEDIUM"
}

fun mapSeverityLocalToRemote(local: String): String = when (local.uppercase()) {
    "LOW" -> "bajo"
    "MEDIUM" -> "medio"
    "HIGH" -> "alto"
    else -> "medio"
}

// ---------------------------------------------------------------------------
// PUSH: Room → Supabase
// ---------------------------------------------------------------------------

fun AttendanceEntity.toAsistenciaInsertDto(defaultCourseId: Long): AsistenciaInsertDto = AsistenciaInsertDto(
    uuid = uuid,
    estudianteId = studentId,
    cursoId = courseId ?: defaultCourseId,
    materiaId = subjectId,
    fecha = date,
    horaEntrada = entryTime,
    estado = status.toRemoteAttendanceStatus(),
    observacion = observation,
    justificacion = justification,
    docenteId = teacherId,
    sincronizacionPendiente = false
)

fun GradeEntity.toCalificacionInsertDto(
    defaultCourseId: Long,
    defaultMateriaId: Long,
    defaultTipoEvaluacionId: Long,
    defaultPeriodoId: Long?
): CalificacionInsertDto = CalificacionInsertDto(
    uuid = uuid,
    estudianteId = studentId,
    materiaId = subjectId ?: defaultMateriaId,
    cursoId = courseId ?: defaultCourseId,
    periodoAcademicoId = periodAcademicId ?: defaultPeriodoId,
    tipoEvaluacionId = evaluationTypeId ?: defaultTipoEvaluacionId,
    descripcion = description,
    notaObtenida = score,
    notaMaxima = maxScore,
    observacion = observation,
    docenteId = teacherId,
    estado = state
)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun String.toRemoteAttendanceStatus(): String = when (this.lowercase()) {
    "presente", "present" -> "presente"
    "atrasado", "late" -> "atrasado"
    "ausente", "absent" -> "ausente"
    "justificado", "justified" -> "justificado"
    else -> "presente"
}

fun AttendanceStatus.toRemoteStatus(): String = when (this) {
    AttendanceStatus.PRESENT -> "presente"
    AttendanceStatus.LATE -> "atrasado"
    AttendanceStatus.ABSENT -> "ausente"
    AttendanceStatus.JUSTIFIED -> "justificado"
    AttendanceStatus.UNMARKED -> "ausente"
}

/**
 * Convierte un timestamp ISO-8601 (TIMESTAMPTZ Postgres) a epoch millis.
 * Tolera valores nulos y strings malformados (devuelve null).
 */
fun String.toEpochMillisOrNull(): Long? = runCatching {
    Instant.parse(this).toEpochMilli()
}.getOrNull()
