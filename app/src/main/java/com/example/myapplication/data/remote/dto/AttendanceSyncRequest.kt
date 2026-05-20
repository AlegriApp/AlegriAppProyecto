package com.example.myapplication.data.remote.dto

data class AttendanceSyncRequest(
    val date: String,
    val courseName: String,
    val records: List<Record>
) {
    data class Record(
        val attendanceId: Long,
        val studentId: Long,
        val status: String,
        val updatedAt: Long
    )
}
