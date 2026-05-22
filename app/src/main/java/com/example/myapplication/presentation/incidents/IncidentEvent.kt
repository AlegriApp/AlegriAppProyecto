package com.example.myapplication.presentation.incidents

import com.example.myapplication.domain.model.IncidentSeverity
import com.example.myapplication.domain.model.IncidentType

sealed interface IncidentEvent {
    data object LoadStudents : IncidentEvent
    data class StudentSelected(val studentId: Long) : IncidentEvent
    data class TypeSelected(val type: IncidentType) : IncidentEvent
    data class SeveritySelected(val severity: IncidentSeverity) : IncidentEvent
    data class DescriptionChanged(val description: String) : IncidentEvent
    data object SaveIncidentClicked : IncidentEvent
    data object SendReportClicked : IncidentEvent
    data object ClearForm : IncidentEvent
    data object ClearMessages : IncidentEvent
}
