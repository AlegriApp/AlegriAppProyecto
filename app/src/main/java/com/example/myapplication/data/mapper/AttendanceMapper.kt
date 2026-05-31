package com.example.myapplication.data.mapper

import com.example.myapplication.core.common.newUuid
import com.example.myapplication.data.local.entity.AttendanceEntity
import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.AttendanceStatus
import com.example.myapplication.domain.model.sync.SyncState

fun AttendanceEntity.toDomain(): Attendance = Attendance(
    id = id,
    studentId = studentId,
    courseId = courseId,
    subjectId = subjectId,
    teacherId = teacherId,
    date = date,
    entryTime = entryTime,
    status = status.toAttendanceStatus(),
    observation = observation,
    justification = justification,
    syncPending = syncPending
)

/**
 * Convierte un [Attendance] de dominio en [AttendanceEntity].
 *
 * @param existing Entidad existente en Room (si la hay). Se reutiliza su `id`,
 *                 `uuid`, `remoteId` y `serverUpdatedAt` para no perderlos.
 * @param markPending Si true (default), marca la entidad como `IDLE` (a sync).
 *                    Útil al guardar cambios locales. Pasar false al hidratar
 *                    desde el servidor donde queremos `SUCCESS`.
 */
fun Attendance.toEntity(
    existing: AttendanceEntity? = null,
    markPending: Boolean = true
): AttendanceEntity = AttendanceEntity(
    id = existing?.id ?: id,
    studentId = studentId,
    courseId = courseId,
    subjectId = subjectId,
    teacherId = teacherId,
    date = date,
    entryTime = entryTime,
    status = status.toDatabaseStatus(),
    observation = observation,
    justification = justification,
    syncPending = markPending,
    uuid = existing?.uuid ?: newUuid(),
    remoteId = existing?.remoteId,
    syncStatus = if (markPending) SyncState.Stored.IDLE else SyncState.Stored.SUCCESS,
    syncError = if (markPending) null else existing?.syncError,
    lastSyncAttempt = existing?.lastSyncAttempt,
    serverUpdatedAt = existing?.serverUpdatedAt,
    isDeleted = existing?.isDeleted ?: false
)

private fun String.toAttendanceStatus(): AttendanceStatus = when (lowercase()) {
    "presente", "present" -> AttendanceStatus.PRESENT
    "atrasado", "late" -> AttendanceStatus.LATE
    "ausente", "absent" -> AttendanceStatus.ABSENT
    "justificado", "justified" -> AttendanceStatus.JUSTIFIED
    else -> AttendanceStatus.UNMARKED
}

private fun AttendanceStatus.toDatabaseStatus(): String = when (this) {
    AttendanceStatus.PRESENT -> "presente"
    AttendanceStatus.LATE -> "atrasado"
    AttendanceStatus.ABSENT -> "ausente"
    AttendanceStatus.JUSTIFIED -> "justificado"
    AttendanceStatus.UNMARKED -> error("No se puede persistir asistencia sin marcar")
}
