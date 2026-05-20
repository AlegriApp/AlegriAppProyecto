package com.example.myapplication.presentation.grades

import com.example.myapplication.presentation.grades.components.GradeStudentMock
import com.example.myapplication.presentation.grades.components.GradeVisualStatus
import com.example.myapplication.presentation.grades.components.gradesMockStudents

data class GradesUiState(
    val selectedSubject: String = "Matemáticas",
    val selectedPeriod: String = "1er Lapso",
    val courseName: String = "5to Grado Sección A",
    val students: List<GradeStudentMock> = gradesMockStudents
) {
    val sectionAverage: Double
        get() = students.mapNotNull { it.score }.average().takeIf { !it.isNaN() } ?: 0.0

    val totalStudents: Int
        get() = students.size

    val atRiskStudents: Int
        get() = students.count { it.status == GradeVisualStatus.AT_RISK }

    val pendingBulletins: Int
        get() = students.count { it.status == GradeVisualStatus.NOT_REGISTERED }
}
