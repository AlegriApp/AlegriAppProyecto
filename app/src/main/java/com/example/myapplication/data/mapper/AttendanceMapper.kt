package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.AttendanceEntity
import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.AttendanceStatus

fun AttendanceEntity.toDomain(): Attendance = Attendance(
    id = id,
    studentId = studentId,
    date = date,
    status = status.toAttendanceStatus(),
    synced = synced,
    updatedAt = updatedAt
)

fun Attendance.toEntity(): AttendanceEntity = AttendanceEntity(
    id = id,
    studentId = studentId,
    date = date,
    status = status.name,
    synced = synced,
    updatedAt = updatedAt
)

private fun String.toAttendanceStatus(): AttendanceStatus =
    runCatching { AttendanceStatus.valueOf(this) }.getOrDefault(AttendanceStatus.UNMARKED)
