package com.example.myapplication.presentation.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.example.myapplication.BuildConfig
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.sync.SyncOutcome
import com.example.myapplication.domain.repository.SyncRepository
import com.example.myapplication.domain.model.AttendanceStatus
import com.example.myapplication.domain.usecase.attendance.GetAttendanceByDateUseCase
import com.example.myapplication.domain.usecase.ocr.RecognizeTextFromImageUseCase
import com.example.myapplication.domain.usecase.attendance.SaveAttendanceUseCase
import com.example.myapplication.domain.model.telegram.TelegramSendOutcome
import com.example.myapplication.domain.usecase.telegram.SendTelegramMessageUseCase
import com.example.myapplication.services.telegram.TelegramMessageBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

class AttendanceViewModel(
    private val getAttendanceByDateUseCase: GetAttendanceByDateUseCase,
    private val saveAttendanceUseCase: SaveAttendanceUseCase,
    private val recognizeTextFromImageUseCase: RecognizeTextFromImageUseCase,
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase,
    private val networkMonitor: NetworkMonitor? = null,
    private val syncRepository: SyncRepository? = null,
    initialState: AttendanceUiState = AttendanceUiState(
        isLoading = true,
        selectedDate = currentDate(),
        dateLabel = "Fecha: ${currentDateLabel()}"
    )
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null
    private var hasPendingLocalEdits = false

    init {
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.isOnline.collect { online ->
                    _uiState.update { it.copy(isOffline = !online) }
                }
            }
        }
        observeAttendanceForDate(_uiState.value.selectedDate)
    }

    fun onEvent(event: AttendanceEvent) {
        when (event) {
            AttendanceEvent.LoadStudents -> observeAttendanceForDate(_uiState.value.selectedDate)
            is AttendanceEvent.ChangeDate -> changeDate(event.selectedDate)
            is AttendanceEvent.MarkPresent -> updateStatus(event.studentId, AttendanceStatus.PRESENT)
            is AttendanceEvent.MarkLate -> updateStatus(event.studentId, AttendanceStatus.LATE)
            is AttendanceEvent.MarkAbsent -> updateStatus(event.studentId, AttendanceStatus.ABSENT)
            is AttendanceEvent.MarkJustified -> updateStatus(event.studentId, AttendanceStatus.JUSTIFIED)
            AttendanceEvent.MarkAllPresent -> markAllPresent()
            AttendanceEvent.ClearMarks -> clearMarks()
            AttendanceEvent.SaveAttendance -> saveAttendance()
            AttendanceEvent.SendReport -> sendReport()
            is AttendanceEvent.OcrImageSelected -> processOcr(event.uri)
            AttendanceEvent.ApplyOcrSuggestions -> applyOcrSuggestions()
            AttendanceEvent.ClearMessages -> clearMessages()
        }
    }

    private fun observeAttendanceForDate(date: String) {
        observeJob?.cancel()
        hasPendingLocalEdits = false
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            getAttendanceByDateUseCase(date).collect { records ->
                val students = records.map { record ->
                    AttendanceStudentUi(
                        id = record.student.id,
                        name = record.student.fullName,
                        gradeSection = "${record.student.grade} ${record.student.section}"
                    )
                }
                val statusFromDb = records
                    .mapNotNull { record ->
                        record.attendance?.let { attendance ->
                            attendance.studentId to attendance.status
                        }
                    }
                    .toMap()
                val courseName = records.firstOrNull()?.student?.let { student ->
                    "${student.grade} Grado Sección ${student.section}"
                } ?: "Curso no asignado"
                _uiState.update { state ->
                    val attendanceByStudent = if (hasPendingLocalEdits) {
                        state.attendanceByStudent
                    } else {
                        statusFromDb
                    }
                    state.copy(
                        selectedDate = date,
                        dateLabel = "Fecha: ${toHumanDate(date)}",
                        students = students,
                        attendanceByStudent = attendanceByStudent,
                        courseName = courseName,
                        isLoading = false
                    ).recalculateSummary()
                }
            }
        }
    }

    private fun changeDate(selectedDate: String) {
        observeAttendanceForDate(selectedDate)
    }

    private fun updateStatus(studentId: Long, status: AttendanceStatus) {
        hasPendingLocalEdits = true
        _uiState.update { state ->
            state.withSelectedStatus(studentId, status).copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun markAllPresent() {
        hasPendingLocalEdits = true
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
        hasPendingLocalEdits = true
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
            val current = _uiState.value
            if (current.students.isEmpty()) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "No hay estudiantes para registrar.") }
                return@launch
            }
            val hasUnmarkedStudents = current.students.any { student ->
                current.attendanceByStudent[student.id] == null ||
                    current.attendanceByStudent[student.id] == AttendanceStatus.UNMARKED
            }
            if (hasUnmarkedStudents) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Debes marcar asistencia de todos los estudiantes antes de guardar."
                    )
                }
                return@launch
            }
            runCatching {
                current.students.forEach { student ->
                    val status = current.attendanceByStudent[student.id] ?: AttendanceStatus.UNMARKED
                    saveAttendanceUseCase(
                        Attendance(
                            studentId = student.id,
                            date = current.selectedDate,
                            status = status,
                            syncPending = true
                        )
                    )
                }
            }.onSuccess {
                hasPendingLocalEdits = false
                val syncMessage = syncRepository?.syncPendingRecords()?.let { outcome ->
                    when (outcome) {
                        is SyncOutcome.Success -> " ${outcome.message}"
                        is SyncOutcome.Skipped -> ""
                        is SyncOutcome.Failure -> " (sync: ${outcome.message})"
                    }
                }.orEmpty()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "Asistencia guardada correctamente.$syncMessage"
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "No se pudo guardar la asistencia."
                    )
                }
            }
        }
    }

    private fun sendReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null, successMessage = null) }
            val currentState = _uiState.value
            val hasUnmarkedStudents = currentState.students.any { student ->
                currentState.attendanceByStudent[student.id] == null ||
                    currentState.attendanceByStudent[student.id] == AttendanceStatus.UNMARKED
            }
            if (hasUnmarkedStudents) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = "No se puede enviar el reporte: hay estudiantes sin marcar."
                    )
                }
                return@launch
            }
            delay(100)
            val entries = currentState.students.map { student ->
                AttendanceReportEntry(
                    studentId = student.id,
                    studentName = student.name,
                    status = currentState.attendanceByStudent[student.id] ?: AttendanceStatus.UNMARKED
                )
            }
            val reportPreview = AttendanceReportPreview(
                dateLabel = currentState.dateLabel,
                courseName = currentState.courseName,
                totalStudents = currentState.students.size,
                summary = currentState.summary,
                entries = entries
            )
            val message = TelegramMessageBuilder.buildAttendanceReport(
                date = currentState.selectedDate,
                courseName = currentState.courseName,
                records = currentState.students.map { student ->
                    val status = currentState.attendanceByStudent[student.id] ?: AttendanceStatus.UNMARKED
                    val domainAttendance = Attendance(
                        studentId = student.id,
                        date = currentState.selectedDate,
                        status = status
                    )
                    val domainStudent = com.example.myapplication.domain.model.Student(
                        id = student.id,
                        fullName = student.name,
                        grade = student.gradeSection,
                        section = "",
                        representativeName = ""
                    )
                    domainStudent to domainAttendance
                }
            )
            val chatId = BuildConfig.TELEGRAM_DEFAULT_CHAT_ID
            when (val outcome = sendTelegramMessageUseCase(chatId, message)) {
                is TelegramSendOutcome.Success -> {
                    _uiState.update {
                        currentState.copy(
                            isSending = false,
                            reportPreview = reportPreview,
                            successMessage = "Reporte enviado por Telegram.",
                            errorMessage = null
                        )
                    }
                }
                is TelegramSendOutcome.Failure -> {
                    _uiState.update {
                        currentState.copy(
                            isSending = false,
                            reportPreview = reportPreview,
                            successMessage = null,
                            errorMessage = outcome.message
                        )
                    }
                }
            }
        }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun processOcr(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingOcr = true,
                    errorMessage = null,
                    successMessage = null
                )
            }
            val result = recognizeTextFromImageUseCase(uri)
            result.onSuccess { ocr ->
                _uiState.update {
                    it.copy(
                        isProcessingOcr = false,
                        detectedOcrText = ocr.rawText,
                        successMessage = "Texto detectado. Revisa y aplica sugerencias."
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isProcessingOcr = false,
                        errorMessage = throwable.message ?: "No se pudo leer el texto de la imagen."
                    )
                }
            }
        }
    }

    private fun applyOcrSuggestions() {
        hasPendingLocalEdits = true
        val state = _uiState.value
        if (state.detectedOcrText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "No hay texto OCR para procesar.") }
            return
        }
        val lines = state.detectedOcrText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        val updates = state.students.associate { student ->
            student.id to inferStatusFromOcr(student.name, lines)
        }
        _uiState.update {
            it.copy(
                attendanceByStudent = updates,
                successMessage = "Sugerencias OCR aplicadas. Verifica antes de guardar."
            ).recalculateSummary()
        }
    }

    private fun inferStatusFromOcr(studentName: String, lines: List<String>): AttendanceStatus {
        val studentKey = studentName.lowercase()
        val studentLine = lines.firstOrNull { line ->
            line.lowercase().contains(studentKey)
        } ?: return AttendanceStatus.UNMARKED
        val normalized = studentLine.lowercase()
        return when {
            normalized.contains("presente") -> AttendanceStatus.PRESENT
            normalized.contains("atrasado") || normalized.contains("tarde") -> AttendanceStatus.LATE
            normalized.contains("justificado") -> AttendanceStatus.JUSTIFIED
            normalized.contains("ausente") || normalized.contains("falta") -> AttendanceStatus.ABSENT
            else -> AttendanceStatus.UNMARKED
        }
    }

    companion object {
        private fun currentDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        private fun currentDateLabel(): String = toHumanDate(currentDate())

        private fun toHumanDate(date: String): String {
            val localDate = runCatching { LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) }
                .getOrElse { return date }
            return localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    }
}
