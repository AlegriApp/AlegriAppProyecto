package com.example.myapplication.domain.model

data class Grade(
    val id: Long = 0L,
    val studentId: Long,
    val subjectId: Long? = null,
    val courseId: Long? = null,
    val periodAcademicId: Long? = null,
    val evaluationTypeId: Long? = null,
    val subject: String,
    val period: String,
    val activityName: String,
    val activityType: String,
    val score: Double,
    val maxScore: Double,
    val observation: String? = null,
    val teacherId: Long? = null,
    val state: String = "registrado",
    val syncPending: Boolean = false
) {
    init {
        require(subject.isNotBlank()) { "subject must not be blank" }
        require(period.isNotBlank()) { "period must not be blank" }
        require(activityName.isNotBlank()) { "activityName must not be blank" }
        require(activityType.isNotBlank()) { "activityType must not be blank" }
        require(maxScore > 0) { "maxScore must be > 0" }
        require(score in 0.0..maxScore) { "score must be between 0 and maxScore" }
    }
}
