package com.example.myapplication.data.remote.dto

data class AttendanceSyncRequest(
    val date: String,
    val grade: String,
    val section: String,
    val records: List<Record>,
    val syncMetadata: SyncMetadata = SyncMetadata()
) {
    data class Record(
        val attendanceId: Long,
        val studentId: Long,
        val status: String
    )

    data class SyncMetadata(
        val requestedAt: Long = System.currentTimeMillis(),
        val source: String = "mobile_offline_queue"
    )
}
