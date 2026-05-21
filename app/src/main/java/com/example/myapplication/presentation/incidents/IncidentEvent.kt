package com.example.myapplication.presentation.incidents

sealed interface IncidentEvent {
    data class StudentSelected(val studentId: Long) : IncidentEvent
    data class TypeSelected(val type: IncidentTypeOption) : IncidentEvent
    data class DescriptionChanged(val description: String) : IncidentEvent
    data object SendReportClicked : IncidentEvent
}
