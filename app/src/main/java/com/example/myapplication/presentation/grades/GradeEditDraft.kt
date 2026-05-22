package com.example.myapplication.presentation.grades

data class GradeEditDraft(
    val studentId: Long,
    val activityName: String = DEFAULT_ACTIVITY_NAME,
    val activityType: String = DEFAULT_ACTIVITY_TYPE,
    val score: Double,
    val maxScore: Double = DEFAULT_MAX_SCORE
) {
    companion object {
        const val DEFAULT_ACTIVITY_NAME = "Registro docente"
        const val DEFAULT_ACTIVITY_TYPE = "Manual"
        const val DEFAULT_MAX_SCORE = 20.0
    }
}
