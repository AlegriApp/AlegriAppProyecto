package com.example.myapplication.domain.model

data class Grade(
    val id: Long = 0L,
    val studentId: Long,
    val subject: String,
    val period: String,
    val activity: String,
    val score: Double? = null,
    val synced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(subject.isNotBlank()) { "subject must not be blank" }
        require(period.isNotBlank()) { "period must not be blank" }
        require(activity.isNotBlank()) { "activity must not be blank" }
        require(score == null || score in 0.0..20.0) { "score must be between 0.0 and 20.0" }
    }
}
