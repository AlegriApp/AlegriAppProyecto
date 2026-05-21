package com.example.myapplication.presentation.incidents

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IncidentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentUiState())
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()

    fun onEvent(event: IncidentEvent) {
        when (event) {
            is IncidentEvent.StudentSelected -> {
                _uiState.value = _uiState.value.copy(selectedStudentId = event.studentId)
            }

            is IncidentEvent.TypeSelected -> {
                _uiState.value = _uiState.value.copy(selectedType = event.type)
            }

            is IncidentEvent.DescriptionChanged -> {
                _uiState.value = _uiState.value.copy(description = event.description)
            }

            IncidentEvent.SendReportClicked -> Unit
        }
    }
}
