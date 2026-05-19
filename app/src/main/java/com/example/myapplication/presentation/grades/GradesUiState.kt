package com.example.myapplication.presentation.grades

import com.example.myapplication.presentation.grades.components.GradeStudentMock
import com.example.myapplication.presentation.grades.components.gradesMockStudents

data class GradesUiState(
    val selectedSubject: String = "Matemáticas",
    val selectedPeriod: String = "1er Lapso",
    val courseName: String = "5to Grado Sección A",
    val students: List<GradeStudentMock> = gradesMockStudents
)
