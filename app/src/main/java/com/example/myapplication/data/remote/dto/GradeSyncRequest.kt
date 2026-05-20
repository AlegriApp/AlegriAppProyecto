package com.example.myapplication.data.remote.dto

data class GradeSyncRequest(
    val studentId: Long,
    val subject: String,
    val period: String,
    val grades: List<GradeItem>,
    val average: Double,
    val syncMetadata: SyncMetadata = SyncMetadata()
) {
    data class GradeItem(
        val gradeId: Long,
        val activityName: String,
        val activityType: String,
        val score: Double,
        val maxScore: Double
    )

    data class SyncMetadata(
        val syncedAt: Long = System.currentTimeMillis(),
        val source: String = "mobile_offline_queue"
    )
}
