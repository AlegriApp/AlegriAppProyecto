package com.example.myapplication.presentation.grades

import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.model.Student
import com.example.myapplication.presentation.grades.components.GradeStudentMock
import com.example.myapplication.presentation.grades.components.GradeVisualStatus
import com.example.myapplication.presentation.grades.components.gradesPeriods
import com.example.myapplication.presentation.grades.components.gradesSubjects
import com.example.myapplication.presentation.grades.components.gradesMockStudents

data class GradesUiState(
    val isLoading: Boolean = false,
    val studentsDomain: List<Student> = emptyList(),
    val gradesDomain: List<Grade> = emptyList(),
    val selectedSubject: String = "Matemáticas",
    val selectedPeriod: String = "1er Lapso",
    val subjects: List<String> = gradesSubjects,
    val periods: List<String> = gradesPeriods,
    val courseName: String = "5to Grado Sección A",
    val students: List<GradeStudentMock> = gradesMockStudents,
    val grades: Map<Long, Double> = emptyMap(),
    val classAverage: Double = 0.0,
    val approvedCount: Int = 0,
    val riskCount: Int = 0,
    val activitiesCount: Int = 0,
    val isSaving: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isOffline: Boolean = false
) {
    val sectionAverage: Double
        get() = classAverage

    val totalStudents: Int
        get() = students.size

    val atRiskStudents: Int
        get() = riskCount

    val pendingBulletins: Int
        get() = students.count { it.status == GradeVisualStatus.NOT_REGISTERED }
}
