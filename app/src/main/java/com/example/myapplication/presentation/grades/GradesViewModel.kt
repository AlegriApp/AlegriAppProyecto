package com.example.myapplication.presentation.grades

import com.example.myapplication.presentation.grades.components.GradeVisualStatus
import com.example.myapplication.presentation.grades.components.gradesMockStudents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GradesViewModel(
    initialState: GradesUiState = GradesUiState()
) {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    fun onEvent(event: GradesEvent) {
        when (event) {
            is GradesEvent.SubjectSelected -> {
                _uiState.value = _uiState.value.copy(selectedSubject = event.subject)
            }

            is GradesEvent.PeriodSelected -> {
                _uiState.value = _uiState.value.copy(selectedPeriod = event.period)
            }

            is GradesEvent.UpdateStudentScore -> {
                _uiState.value = _uiState.value.withUpdatedScore(event.studentId, event.score)
            }

            is GradesEvent.SaveGrades -> {
                _uiState.value = _uiState.value.copy(
                    selectedSubject = event.subject,
                    selectedPeriod = event.period
                )
            }

            GradesEvent.SyncGrades -> Unit
            is GradesEvent.OpenDetail -> Unit
            GradesEvent.SendBulletinClicked -> Unit
        }
    }

    companion object {
        fun mock(): GradesViewModel = GradesViewModel(
            initialState = GradesUiState(students = gradesMockStudents)
        )
    }
}

private fun GradesUiState.withUpdatedScore(studentId: Long, score: Int?): GradesUiState {
    val normalizedScore = score?.coerceIn(0, 20)
    val updatedStudents = students.map { student ->
        if (student.id != studentId) return@map student
        student.copy(
            score = normalizedScore,
            status = when (normalizedScore) {
                null -> GradeVisualStatus.NOT_REGISTERED
                in 0..9 -> GradeVisualStatus.AT_RISK
                else -> GradeVisualStatus.APPROVED
            }
        )
    }
    return copy(students = updatedStudents)
}
