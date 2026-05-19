        package com.example.myapplication.presentation.grades.components

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
    val maxScore: Int = 20,
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
    generalAverage = 17.4,
    status = GradeVisualStatus.APPROVED,
    approvedSubjects = 3,
    atRiskSubjects = 1,
    lastSyncDate = "18 May 2026 - 09:42",
    subjects = listOf(
        GradeDetailSubject(
            id = 1L,
            name = "Matemáticas",
            average = 18.5,
            status = GradeVisualStatus.APPROVED,
            entries = listOf(
                GradeDetailEntry(
                    id = 1L,
                    activity = "Examen escrito",
                    score = 19.0,
                    maxScore = 20,
                    date = "12 May 2026",
                    teacherComment = "Excelente manejo de operaciones combinadas."
                ),
                GradeDetailEntry(
                    id = 2L,
                    activity = "Tarea: fracciones",
                    score = 18.0,
                    maxScore = 20,
                    date = "06 May 2026"
                ),
                GradeDetailEntry(
                    id = 3L,
                    activity = "Participación en clase",
                    score = 18.5,
                    maxScore = 20,
                    date = "02 May 2026",
                    teacherComment = "Participa de forma constante."
                )
            )
        ),
        GradeDetailSubject(
            id = 2L,
            name = "Lengua",
            average = 17.0,
            status = GradeVisualStatus.APPROVED,
            entries = listOf(
                GradeDetailEntry(
                    id = 4L,
                    activity = "Ensayo: lectura crítica",
                    score = 17.0,
                    maxScore = 20,
                    date = "10 May 2026",
                    teacherComment = "Buen análisis, mejorar la ortografía."
                ),
                GradeDetailEntry(
                    id = 5L,
                    activity = "Dictado",
                    score = 16.5,
                    maxScore = 20,
                    date = "03 May 2026"
                )
            )
        ),
        GradeDetailSubject(
            id = 3L,
            name = "Ciencias",
            average = 18.0,
            status = GradeVisualStatus.APPROVED,
            entries = listOf(
                GradeDetailEntry(
                    id = 6L,
                    activity = "Proyecto: ecosistemas",
                    score = 18.0,
                    maxScore = 20,
                    date = "14 May 2026",
                    teacherComment = "Trabajo creativo y bien fundamentado."
                )
            )
        ),
        GradeDetailSubject(
            id = 4L,
            name = "Estudios Sociales",
            average = 12.0,
            status = GradeVisualStatus.AT_RISK,
            entries = listOf(
                GradeDetailEntry(
                    id = 7L,
                    activity = "Examen parcial",
                    score = 11.0,
                    maxScore = 20,
                    date = "11 May 2026",
                    teacherComment = "Requiere repaso del Tema 2."
                ),
                GradeDetailEntry(
                    id = 8L,
                    activity = "Mapa histórico",
                    score = 13.0,
                    maxScore = 20,
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
