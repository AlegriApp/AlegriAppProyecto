package com.example.myapplication.presentation.grades

import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.model.Student
import com.example.myapplication.presentation.grades.components.GradeStudentMock
import com.example.myapplication.presentation.grades.components.GradeVisualStatus

data class GradesUiState(
    val isLoading: Boolean = true,
    val studentsDomain: List<Student> = emptyList(),
    val gradesDomain: List<Grade> = emptyList(),
    val selectedSubject: String = "",
    val selectedPeriod: String = "",
    val subjects: List<String> = emptyList(),
    val periods: List<String> = emptyList(),
    val courseName: String = "",
    val students: List<GradeStudentMock> = emptyList(),
    val grades: Map<Long, Double> = emptyMap(),
    val classAverage: Double = 0.0,
    val approvedCount: Int = 0,
    val riskCount: Int = 0,
    val activitiesCount: Int = 0,
    val isSaving: Boolean = false,
    val isSending: Boolean = false,
    val isProcessingOcr: Boolean = false,
    val detectedOcrText: String = "",
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
