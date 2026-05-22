package com.example.myapplication.presentation.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.IncidentSeverity
import com.example.myapplication.domain.model.IncidentType
import com.example.myapplication.domain.model.telegram.TelegramSendOutcome
import com.example.myapplication.domain.repository.IncidentRepository
import com.example.myapplication.domain.usecase.incidents.SaveIncidentUseCase
import com.example.myapplication.domain.usecase.incidents.SendIncidentReportUseCase
import com.example.myapplication.domain.usecase.student.GetStudentsUseCase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IncidentViewModel(
    private val getStudentsUseCase: GetStudentsUseCase,
    private val saveIncidentUseCase: SaveIncidentUseCase,
    private val sendIncidentReportUseCase: SendIncidentReportUseCase,
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentUiState())
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        loadStudents()
    }

    fun onEvent(event: IncidentEvent) {
        when (event) {
            IncidentEvent.LoadStudents -> loadStudents()
            is IncidentEvent.StudentSelected -> onStudentSelected(event.studentId)
            is IncidentEvent.TypeSelected -> onTypeSelected(event.type)
            is IncidentEvent.SeveritySelected -> onSeveritySelected(event.severity)
            is IncidentEvent.DescriptionChanged -> onDescriptionChanged(event.description)
            IncidentEvent.SaveIncidentClicked -> saveIncident()
            IncidentEvent.SendReportClicked -> sendIncidentReport()
            IncidentEvent.ClearForm -> clearForm()
            IncidentEvent.ClearMessages -> clearMessages()
        }
    }

    fun loadStudents() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingStudents = true,
                    isLoadingIncidents = true,
                    errorMessage = null
                )
            }
            runCatching {
                combine(
                    getStudentsUseCase(),
                    incidentRepository.observeIncidents()
                ) { students, incidents ->
                    val namesByStudent = students.associate { it.id to it.fullName }
                    IncidentUiState(
                        students = students,
                        incidents = incidents.map { incident ->
                            IncidentHistoryItem(
                                incident = incident,
                                studentName = namesByStudent[incident.studentId] ?: "Estudiante no encontrado"
                            )
                        },
                        selectedStudentId = _uiState.value.selectedStudentId,
                        selectedType = _uiState.value.selectedType,
                        selectedSeverity = _uiState.value.selectedSeverity,
                        description = _uiState.value.description,
                        lastSavedIncidentId = _uiState.value.lastSavedIncidentId,
                        sendStatus = _uiState.value.sendStatus,
                        isLoadingStudents = false,
                        isLoadingIncidents = false,
                        studentError = _uiState.value.studentError,
                        typeError = _uiState.value.typeError,
                        descriptionError = _uiState.value.descriptionError,
                        errorMessage = _uiState.value.errorMessage,
                        successMessage = _uiState.value.successMessage
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingStudents = false,
                        isLoadingIncidents = false,
                        errorMessage = throwable.message ?: "No se pudieron cargar los datos de incidentes."
                    )
                }
            }
        }
    }

    fun onStudentSelected(studentId: Long) {
        _uiState.update {
            it.copy(
                selectedStudentId = studentId,
                lastSavedIncidentId = null,
                sendStatus = IncidentSendStatus.Idle,
                studentError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onTypeSelected(type: IncidentType) {
        _uiState.update {
            it.copy(
                selectedType = type,
                lastSavedIncidentId = null,
                sendStatus = IncidentSendStatus.Idle,
                typeError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onSeveritySelected(severity: IncidentSeverity) {
        _uiState.update {
            it.copy(
                selectedSeverity = severity,
                lastSavedIncidentId = null,
                sendStatus = IncidentSendStatus.Idle,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update {
            it.copy(
                description = description,
                lastSavedIncidentId = null,
                sendStatus = IncidentSendStatus.Idle,
                descriptionError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun saveIncident() {
        viewModelScope.launch {
            if (!validateForm()) return@launch
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val current = _uiState.value
            val incident = buildIncidentFromState(current)
            runCatching {
                saveIncidentUseCase(incident)
            }.onSuccess { incidentId ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        lastSavedIncidentId = incidentId,
                        successMessage = "Incidente guardado localmente.",
                        errorMessage = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "No se pudo guardar el incidente."
                    )
                }
            }
        }
    }

    fun sendIncidentReport() {
        viewModelScope.launch {
            if (!validateForm()) return@launch
            _uiState.update {
                it.copy(
                    sendStatus = IncidentSendStatus.Sending,
                    errorMessage = null,
                    successMessage = null
                )
            }
            val current = _uiState.value
            val student = current.selectedStudent
            if (student == null) {
                _uiState.update {
                    it.copy(
                        sendStatus = IncidentSendStatus.Error("Selecciona un estudiante valido."),
                        studentError = "Selecciona un estudiante.",
                        errorMessage = "Selecciona un estudiante valido."
                    )
                }
                return@launch
            }

            val incidentId = current.lastSavedIncidentId
                ?: runCatching { saveIncidentUseCase(buildIncidentFromState(current)) }
                    .getOrElse { throwable ->
                        _uiState.update {
                            it.copy(
                                sendStatus = IncidentSendStatus.Error(
                                    throwable.message ?: "No se pudo guardar antes de enviar."
                                ),
                                errorMessage = throwable.message ?: "No se pudo guardar antes de enviar."
                            )
                        }
                        return@launch
                    }
            val incident = incidentRepository.getIncidentById(incidentId)
                ?: buildIncidentFromState(current).copy(id = incidentId)

            when (val outcome = sendIncidentReportUseCase(student, incident)) {
                is TelegramSendOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            lastSavedIncidentId = incidentId,
                            sendStatus = IncidentSendStatus.Success,
                            successMessage = "Reporte enviado por Telegram.",
                            errorMessage = null
                        )
                    }
                }
                is TelegramSendOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            lastSavedIncidentId = incidentId,
                            sendStatus = IncidentSendStatus.Error(outcome.message),
                            successMessage = null,
                            errorMessage = outcome.message
                        )
                    }
                }
            }
        }
    }

    fun clearForm() {
        _uiState.update {
            it.copy(
                selectedStudentId = null,
                selectedType = null,
                selectedSeverity = IncidentSeverity.MEDIUM,
                description = "",
                lastSavedIncidentId = null,
                sendStatus = IncidentSendStatus.Idle,
                studentError = null,
                typeError = null,
                descriptionError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun validateForm(): Boolean {
        val current = _uiState.value
        val description = current.description.trim()
        val studentError = when {
            current.selectedStudentId == null -> "Selecciona un estudiante."
            current.selectedStudent == null -> "El estudiante seleccionado no esta disponible."
            else -> null
        }
        val typeError = if (current.selectedType == null) "Selecciona un tipo de incidente." else null
        val descriptionError = when {
            description.isBlank() -> "Describe el incidente."
            description.length < MIN_DESCRIPTION_LENGTH -> "La descripcion debe tener al menos $MIN_DESCRIPTION_LENGTH caracteres."
            else -> null
        }
        _uiState.update {
            it.copy(
                studentError = studentError,
                typeError = typeError,
                descriptionError = descriptionError,
                errorMessage = listOfNotNull(studentError, typeError, descriptionError).firstOrNull()
            )
        }
        return studentError == null && typeError == null && descriptionError == null
    }

    private fun buildIncidentFromState(state: IncidentUiState): Incident = Incident(
        studentId = requireNotNull(state.selectedStudentId),
        type = requireNotNull(state.selectedType),
        severity = state.selectedSeverity,
        description = state.description.trim(),
        dateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        teacherName = "Docente",
        sent = false,
        syncPending = true
    )

    private fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    companion object {
        private const val MIN_DESCRIPTION_LENGTH = 10
    }
}
