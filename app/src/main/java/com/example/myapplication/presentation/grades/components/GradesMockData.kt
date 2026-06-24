package com.example.myapplication.presentation.grades.components

import com.example.myapplication.core.common.GradeScale

data class GradeStudentMock(
    val id: Long,
    val name: String,
    val score: Int?,
    val maxScore: Int = GradeScale.MAX_SCORE_INT,
    val status: GradeVisualStatus
)

enum class GradeVisualStatus {
    APPROVED,
    AT_RISK,
    NOT_REGISTERED
}

// Notas demo sobre 10.
val gradesMockStudents = listOf(
    GradeStudentMock(1L, "María González", 9, status = GradeVisualStatus.APPROVED),
    GradeStudentMock(2L, "Juan Perez", 7, status = GradeVisualStatus.APPROVED),
    GradeStudentMock(3L, "Ana Rodríguez", 5, status = GradeVisualStatus.AT_RISK),
    GradeStudentMock(4L, "Carlos Martínez", null, status = GradeVisualStatus.NOT_REGISTERED),
    GradeStudentMock(5L, "Sofía López", 10, status = GradeVisualStatus.APPROVED),
    GradeStudentMock(6L, "Diego Sánchez", 6, status = GradeVisualStatus.AT_RISK)
)
