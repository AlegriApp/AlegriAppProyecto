        package com.example.myapplication.presentation.grades.components

import com.example.myapplication.core.common.GradeScale

data class GradeDetailStudent(
    val id: Long,
    val fullName: String,
    val course: String,
    val period: String,
    val generalAverage: Double,
    val status: GradeVisualStatus,
    val approvedSubjects: Int,
    val atRiskSubjects: Int,
    val lastSyncDate: String,
    val subjects: List<GradeDetailSubject>
)

data class GradeDetailSubject(
    val id: Long,
    val name: String,
    val average: Double,
    val maxScore: Int = GradeScale.MAX_SCORE_INT,
    val status: GradeVisualStatus,
    val entries: List<GradeDetailEntry>
)

data class GradeDetailEntry(
    val id: Long,
    val activity: String,
    val score: Double,
    val maxScore: Int,
    val date: String,
    val teacherComment: String? = null
)

val gradeDetailMockStudent = GradeDetailStudent(
    id = 1L,
    fullName = "María González Pérez",
    course = "5to Grado - Sección A",
    period = "1er Lapso 2026",
    generalAverage = 8.7,
    status = GradeVisualStatus.APPROVED,
    approvedSubjects = 3,
    atRiskSubjects = 1,
    lastSyncDate = "18 May 2026 - 09:42",
    subjects = listOf(
        GradeDetailSubject(
            id = 1L,
            name = "Matemáticas",
            average = 9.3,
            status = GradeVisualStatus.APPROVED,
            entries = listOf(
                GradeDetailEntry(
                    id = 1L,
                    activity = "Examen escrito",
                    score = 9.5,
                    maxScore = GradeScale.MAX_SCORE_INT,
                    date = "12 May 2026",
                    teacherComment = "Excelente manejo de operaciones combinadas."
                ),
                GradeDetailEntry(
                    id = 2L,
                    activity = "Tarea: fracciones",
                    score = 9.0,
                    maxScore = GradeScale.MAX_SCORE_INT,
                    date = "06 May 2026"
                ),
                GradeDetailEntry(
                    id = 3L,
                    activity = "Participación en clase",
                    score = 9.3,
                    maxScore = GradeScale.MAX_SCORE_INT,
                    date = "02 May 2026",
                    teacherComment = "Participa de forma constante."
                )
            )
        ),
        GradeDetailSubject(
            id = 2L,
            name = "Lengua",
            average = 8.5,
            status = GradeVisualStatus.APPROVED,
            entries = listOf(
                GradeDetailEntry(
                    id = 4L,
                    activity = "Ensayo: lectura crítica",
                    score = 8.5,
                    maxScore = GradeScale.MAX_SCORE_INT,
                    date = "10 May 2026",
                    teacherComment = "Buen análisis, mejorar la ortografía."
                ),
                GradeDetailEntry(
                    id = 5L,
                    activity = "Dictado",
                    score = 8.3,
                    maxScore = GradeScale.MAX_SCORE_INT,
                    date = "03 May 2026"
                )
            )
        ),
        GradeDetailSubject(
            id = 3L,
            name = "Ciencias",
            average = 9.0,
            status = GradeVisualStatus.APPROVED,
            entries = listOf(
                GradeDetailEntry(
                    id = 6L,
                    activity = "Proyecto: ecosistemas",
                    score = 9.0,
                    maxScore = GradeScale.MAX_SCORE_INT,
                    date = "14 May 2026",
                    teacherComment = "Trabajo creativo y bien fundamentado."
                )
            )
        ),
        GradeDetailSubject(
            id = 4L,
            name = "Estudios Sociales",
            average = 6.0,
            status = GradeVisualStatus.AT_RISK,
            entries = listOf(
                GradeDetailEntry(
                    id = 7L,
                    activity = "Examen parcial",
                    score = 5.5,
                    maxScore = GradeScale.MAX_SCORE_INT,
                    date = "11 May 2026",
                    teacherComment = "Requiere repaso del Tema 2."
                ),
                GradeDetailEntry(
                    id = 8L,
                    activity = "Mapa histórico",
                    score = 6.5,
                    maxScore = GradeScale.MAX_SCORE_INT,
                    date = "04 May 2026"
                )
            )
        )
    )
)

fun findMockDetailById(studentId: Long): GradeDetailStudent? {
    val baseStudent = gradesMockStudents.firstOrNull { it.id == studentId }
        ?: return gradeDetailMockStudent.takeIf { studentId == it.id }

    return gradeDetailMockStudent.copy(
        id = baseStudent.id,
        fullName = baseStudent.name,
        status = baseStudent.status,
        generalAverage = baseStudent.score?.toDouble() ?: gradeDetailMockStudent.generalAverage
    )
}
