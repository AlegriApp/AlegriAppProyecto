package com.example.myapplication.presentation.attendance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AttendanceViewModel(
    initialState: AttendanceUiState = attendanceMockUiState()
) {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    fun onEvent(event: AttendanceEvent) {
        when (event) {
            is AttendanceEvent.LoadAttendance -> loadAttendance(event)
            is AttendanceEvent.StatusSelected -> updateStatus(event.studentId, event.status)
            AttendanceEvent.SaveAttendance -> saveAttendance()
            AttendanceEvent.SendReport -> generateReportPreview()
        }
    }

    private fun loadAttendance(event: AttendanceEvent.LoadAttendance) {
        _uiState.value = _uiState.value.copy(
            dateLabel = "Fecha: ${event.date}",
            courseName = event.gradeSection,
            isLoading = false,
            errorMessage = null
        )
    }

    private fun updateStatus(studentId: Long, status: AttendanceStatus) {
        _uiState.value = _uiState.value
            .withSelectedStatus(studentId, status)
            .copy(errorMessage = null)
    }

    private fun saveAttendance() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = null
        )
    }

    private fun generateReportPreview() {
        val currentState = _uiState.value
        val summary = currentState.summary
        val entries = currentState.students.map { student ->
            AttendanceReportEntry(
                studentId = student.id,
                studentName = student.name,
                status = currentState.statusByStudent[student.id] ?: AttendanceStatus.UNMARKED
            )
        }
        _uiState.value = currentState.copy(
            reportPreview = AttendanceReportPreview(
                dateLabel = currentState.dateLabel,
                courseName = currentState.courseName,
                totalStudents = currentState.students.size,
                summary = summary,
                entries = entries
            ),
            errorMessage = null
        )
    }
}
