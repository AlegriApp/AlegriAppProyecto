package com.example.myapplication.data.remote.dto

data class GradeSyncRequest(
    val subject: String,
    val period: String,
    val courseName: String,
    val records: List<Record>
) {
    data class Record(
        val gradeId: Long,
        val studentId: Long,
        val activity: String,
        val score: Double?,
        val updatedAt: Long
    )
}
