package com.example.myapplication.domain.model

data class Attendance(
    val id: Long = 0L,
    val studentId: Long,
    val courseId: Long? = null,
    val subjectId: Long? = null,
    val teacherId: Long? = null,
    val date: String,
    val entryTime: String? = null,
    val status: AttendanceStatus,
    val observation: String? = null,
    val justification: String? = null,
    val syncPending: Boolean = false
)
