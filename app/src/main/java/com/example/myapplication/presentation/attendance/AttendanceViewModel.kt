package com.example.myapplication.presentation.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.AttendanceStatus
import com.example.myapplication.domain.model.Student
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AttendanceViewModel(
    initialState: AttendanceUiState = attendanceMockUiState()
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val mockStudents = listOf(
        Student(1L, "María González", "5to", "A", "Carmen González", "100001"),
        Student(2L, "Juan Pérez", "5to", "A", "Luis Pérez", "100002"),
        Student(3L, "Ana Rodríguez", "5to", "A", "Elena Rodríguez", null),
        Student(4L, "Carlos Martínez", "5to", "A", "José Martínez", "100004"),
        Student(5L, "Sofía López", "5to", "A", "Carla López", null),
        Student(6L, "Diego Sánchez", "5to", "A", "Mario Sánchez", "100006")
    )

    fun onEvent(event: AttendanceEvent) {
        when (event) {
            AttendanceEvent.LoadStudents -> loadStudents()
            is AttendanceEvent.ChangeDate -> changeDate(event.selectedDate)
            is AttendanceEvent.MarkPresent -> updateStatus(event.studentId, AttendanceStatus.PRESENT)
            is AttendanceEvent.MarkLate -> updateStatus(event.studentId, AttendanceStatus.LATE)
            is AttendanceEvent.MarkAbsent -> updateStatus(event.studentId, AttendanceStatus.ABSENT)
            AttendanceEvent.MarkAllPresent -> markAllPresent()
            AttendanceEvent.ClearMarks -> clearMarks()
            AttendanceEvent.SaveAttendance -> saveAttendance()
            AttendanceEvent.SendReport -> sendReport()
            AttendanceEvent.ClearMessages -> clearMessages()
        }
    }

    private fun loadStudents() {
        _uiState.update { state ->
            state.copy(
                students = mockStudents.map { student ->
                    AttendanceStudentUi(
                        id = student.id,
                        name = student.fullName,
                        gradeSection = "${student.grade} ${student.section}"
                    )
                },
                courseName = "${mockStudents.first().grade} Grado Sección ${mockStudents.first().section}",
                isLoading = false
            ).recalculateSummary()
        }
    }

    private fun changeDate(selectedDate: String) {
        _uiState.update { state ->
            state.copy(
                selectedDate = selectedDate,
                dateLabel = "Fecha: $selectedDate",
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun updateStatus(studentId: Long, status: AttendanceStatus) {
        _uiState.update { state ->
            state.withSelectedStatus(studentId, status).copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun markAllPresent() {
        _uiState.update { state ->
            val allPresent = state.students.associate { it.id to AttendanceStatus.PRESENT }
            state.copy(
                attendanceByStudent = allPresent,
                successMessage = null,
                errorMessage = null
            ).recalculateSummary()
        }
    }

    private fun clearMarks() {
        _uiState.update { state ->
            state.copy(
                attendanceByStudent = emptyMap(),
                successMessage = null,
                errorMessage = null
            ).recalculateSummary()
        }
    }

    private fun saveAttendance() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            delay(150)
            _uiState.update { current ->
                if (current.students.isEmpty()) {
                    current.copy(isSaving = false, errorMessage = "No hay estudiantes para registrar.")
                } else {
                    current.copy(isSaving = false, successMessage = "Asistencia guardada localmente.")
                }
            }
        }
    }

    private fun sendReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null, successMessage = null) }
            delay(150)
            val currentState = _uiState.value
            val entries = currentState.students.map { student ->
                AttendanceReportEntry(
                    studentId = student.id,
                    studentName = student.name,
                    status = currentState.attendanceByStudent[student.id] ?: AttendanceStatus.UNMARKED
                )
            }
            _uiState.update {
                currentState.copy(
                    isSending = false,
                    reportPreview = AttendanceReportPreview(
                        dateLabel = currentState.dateLabel,
                        courseName = currentState.courseName,
                        totalStudents = currentState.students.size,
                        summary = currentState.summary,
                        entries = entries
                    ),
                    successMessage = "Reporte preparado para envío."
                )
            }
        }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
