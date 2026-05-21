package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.AttendanceEntity
import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.AttendanceStatus

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

fun Attendance.toEntity(): AttendanceEntity = AttendanceEntity(
    id = id,
    studentId = studentId,
    courseId = courseId,
    subjectId = subjectId,
    teacherId = teacherId,
    date = date,
    entryTime = entryTime,
    status = status.toDatabaseStatus(),
    observation = observation,
    justification = justification,
    syncPending = syncPending
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
    AttendanceStatus.UNMARKED -> "ausente"
}
