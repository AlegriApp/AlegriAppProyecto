package com.example.myapplication.presentation.incidents

import android.net.Uri
import com.example.myapplication.domain.model.IncidentSeverity

sealed interface IncidentEvent {
    data object LoadStudents : IncidentEvent
    data class StudentSelected(val studentId: Long) : IncidentEvent
    data class CourseSelected(val courseId: Long) : IncidentEvent
    data class TypeSelected(val typeId: Long) : IncidentEvent
    data class SeveritySelected(val severity: IncidentSeverity) : IncidentEvent
    data class DescriptionChanged(val description: String) : IncidentEvent
    data class OcrImageSelected(val uri: Uri) : IncidentEvent
    data object ApplyOcrSuggestions : IncidentEvent
    data class ToggleManualStudentForm(val enabled: Boolean) : IncidentEvent
    data class ManualStudentNameChanged(val value: String) : IncidentEvent
    data class ManualStudentGradeChanged(val value: String) : IncidentEvent
    data class ManualStudentSectionChanged(val value: String) : IncidentEvent
    data class ManualRepresentativeChanged(val value: String) : IncidentEvent
    data object SaveIncidentClicked : IncidentEvent
    data object SendReportClicked : IncidentEvent
    data object ClearForm : IncidentEvent
    data object ClearMessages : IncidentEvent
}
