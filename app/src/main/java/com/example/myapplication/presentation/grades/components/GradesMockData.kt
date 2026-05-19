package com.example.myapplication.presentation.grades.components

data class GradeStudentMock(
    val id: Long,
    val name: String,
    val score: Int?,
    val maxScore: Int = 20,
    val status: GradeVisualStatus
)

enum class GradeVisualStatus {
    APPROVED,
    AT_RISK,
    NOT_REGISTERED
}

val gradesSubjects = listOf("Matemáticas", "Lengua", "Ciencias", "Estudios Sociales")
val gradesPeriods = listOf("1er Lapso", "2do Lapso", "3er Lapso")

val gradesMockStudents = listOf(
    GradeStudentMock(1L, "María González", 18, status = GradeVisualStatus.APPROVED),
    GradeStudentMock(2L, "Juan Perez", 14, status = GradeVisualStatus.APPROVED),
    GradeStudentMock(3L, "Ana Rodríguez", 9, status = GradeVisualStatus.AT_RISK),
    GradeStudentMock(4L, "Carlos Martínez", null, status = GradeVisualStatus.NOT_REGISTERED),
    GradeStudentMock(5L, "Sofía López", 19, status = GradeVisualStatus.APPROVED),
    GradeStudentMock(6L, "Diego Sánchez", 11, status = GradeVisualStatus.AT_RISK)
)
