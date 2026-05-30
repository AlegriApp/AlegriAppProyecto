package com.example.myapplication.presentation.incidents

import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.IncidentSeverity
import com.example.myapplication.domain.model.IncidentType
import com.example.myapplication.domain.model.Student

data class IncidentUiState(
    val screenTitle: String = "Reporte de Incidentes",
    val screenDescription: String = "Envio directo a autoridades y representantes via Telegram",
    val students: List<Student> = emptyList(),
    val incidents: List<IncidentHistoryItem> = emptyList(),
    val selectedStudentId: Long? = null,
    val selectedType: IncidentType? = null,
    val selectedSeverity: IncidentSeverity = IncidentSeverity.MEDIUM,
    val description: String = "",
    val lastSavedIncidentId: Long? = null,
    val isLoadingStudents: Boolean = false,
    val isLoadingIncidents: Boolean = false,
    val isProcessingOcr: Boolean = false,
    val isSaving: Boolean = false,
    val detectedOcrText: String = "",
    val ocrSuggestedStudentName: String? = null,
    val ocrMatchMessage: String? = null,
    val hasManualStudentInputSinceLastOcr: Boolean = false,
    val hasManualTypeSelectionSinceLastOcr: Boolean = false,
    val hasManualSeveritySelectionSinceLastOcr: Boolean = false,
    val hasManualDescriptionEditSinceLastOcr: Boolean = false,
    val showManualStudentForm: Boolean = false,
    val manualStudentDraft: ManualStudentDraft = ManualStudentDraft(),
    val sendStatus: IncidentSendStatus = IncidentSendStatus.Idle,
    val studentError: String? = null,
    val manualStudentError: String? = null,
    val typeError: String? = null,
    val descriptionError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val selectedStudent: Student?
        get() = students.firstOrNull { it.id == selectedStudentId }

    val canSubmit: Boolean
        get() = !isLoadingStudents && !isSaving && !isProcessingOcr && sendStatus !is IncidentSendStatus.Sending
}

data class ManualStudentDraft(
    val fullName: String = "",
    val grade: String = "",
    val section: String = "",
    val representativeName: String = ""
)

data class IncidentHistoryItem(
    val incident: Incident,
    val studentName: String
)

sealed interface IncidentSendStatus {
    data object Idle : IncidentSendStatus
    data object Sending : IncidentSendStatus
    data object Success : IncidentSendStatus
    data class Error(val message: String) : IncidentSendStatus
}
