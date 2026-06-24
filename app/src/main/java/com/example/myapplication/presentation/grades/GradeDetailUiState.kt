package com.example.myapplication.presentation.grades

import com.example.myapplication.domain.model.Grade
import com.example.myapplication.presentation.grades.components.GradeDetailStudent

sealed interface GradeDetailUiState {
    data object Loading : GradeDetailUiState
    data object Empty : GradeDetailUiState
    data object Offline : GradeDetailUiState
    data class Error(val message: String) : GradeDetailUiState
    data class Success(
        val student: GradeDetailStudent,
        val grades: List<Grade> = emptyList(),
        val isFromCache: Boolean = false
    ) : GradeDetailUiState
}
