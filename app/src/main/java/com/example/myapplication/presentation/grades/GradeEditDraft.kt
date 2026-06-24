package com.example.myapplication.presentation.grades

import com.example.myapplication.core.common.GradeScale

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
        // Escala oficial sobre 10 (antes 20.0).
        const val DEFAULT_MAX_SCORE = GradeScale.MAX_SCORE
    }
}
