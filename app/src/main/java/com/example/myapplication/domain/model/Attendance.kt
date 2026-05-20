package com.example.myapplication.domain.model

data class Attendance(
    val id: Long = 0L,
    val studentId: Long,
    val date: String,
    val status: AttendanceStatus,
    val synced: Boolean = false
)
