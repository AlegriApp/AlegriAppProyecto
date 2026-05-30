package com.example.myapplication.presentation.incidents

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.IncidentSeverity
import com.example.myapplication.domain.model.IncidentType
import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.model.telegram.TelegramSendOutcome
import com.example.myapplication.domain.repository.IncidentRepository
import com.example.myapplication.domain.repository.StudentRepository
import com.example.myapplication.domain.usecase.incidents.SaveIncidentUseCase
import com.example.myapplication.domain.usecase.incidents.SendIncidentReportUseCase
import com.example.myapplication.domain.usecase.ocr.RecognizeTextFromImageUseCase
import com.example.myapplication.domain.usecase.student.GetStudentsUseCase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.text.Normalizer
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
    private val incidentRepository: IncidentRepository,
    private val studentRepository: StudentRepository,
    private val recognizeTextFromImageUseCase: RecognizeTextFromImageUseCase
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
            is IncidentEvent.OcrImageSelected -> processOcr(event.uri)
            IncidentEvent.ApplyOcrSuggestions -> applyOcrSuggestions()
            is IncidentEvent.ToggleManualStudentForm -> toggleManualStudentForm(event.enabled)
            is IncidentEvent.ManualStudentNameChanged -> updateManualStudentDraft { copy(fullName = event.value) }
            is IncidentEvent.ManualStudentGradeChanged -> updateManualStudentDraft { copy(grade = event.value) }
            is IncidentEvent.ManualStudentSectionChanged -> updateManualStudentDraft { copy(section = event.value) }
            is IncidentEvent.ManualRepresentativeChanged -> updateManualStudentDraft { copy(representativeName = event.value) }
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
                        isProcessingOcr = _uiState.value.isProcessingOcr,
                        detectedOcrText = _uiState.value.detectedOcrText,
                        ocrSuggestedStudentName = _uiState.value.ocrSuggestedStudentName,
                        ocrMatchMessage = _uiState.value.ocrMatchMessage,
                        hasManualStudentInputSinceLastOcr = _uiState.value.hasManualStudentInputSinceLastOcr,
                        hasManualTypeSelectionSinceLastOcr = _uiState.value.hasManualTypeSelectionSinceLastOcr,
                        hasManualSeveritySelectionSinceLastOcr = _uiState.value.hasManualSeveritySelectionSinceLastOcr,
                        hasManualDescriptionEditSinceLastOcr = _uiState.value.hasManualDescriptionEditSinceLastOcr,
                        showManualStudentForm = _uiState.value.showManualStudentForm,
                        manualStudentDraft = _uiState.value.manualStudentDraft,
                        sendStatus = _uiState.value.sendStatus,
                        isLoadingStudents = false,
                        isLoadingIncidents = false,
                        studentError = _uiState.value.studentError,
                        manualStudentError = _uiState.value.manualStudentError,
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
                showManualStudentForm = false,
                hasManualStudentInputSinceLastOcr = true,
                lastSavedIncidentId = null,
                sendStatus = IncidentSendStatus.Idle,
                studentError = null,
                manualStudentError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onTypeSelected(type: IncidentType) {
        _uiState.update {
            it.copy(
                selectedType = type,
                hasManualTypeSelectionSinceLastOcr = true,
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
                hasManualSeveritySelectionSinceLastOcr = true,
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
                hasManualDescriptionEditSinceLastOcr = true,
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
            val studentId = resolveStudentId(current)
                ?: run {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            studentError = "Selecciona o registra un estudiante antes de guardar.",
                            manualStudentError = "No se pudo resolver el estudiante del incidente.",
                            errorMessage = "Selecciona o registra un estudiante antes de guardar."
                        )
                    }
                    return@launch
                }
            val incident = buildIncidentFromState(current, studentId)
            runCatching {
                saveIncidentUseCase(incident)
            }.onSuccess { incidentId ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        selectedStudentId = studentId,
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
            val studentId = resolveStudentId(current)
                ?: run {
                    _uiState.update {
                        it.copy(
                            sendStatus = IncidentSendStatus.Error("Selecciona o registra un estudiante valido."),
                            studentError = "Selecciona o registra un estudiante.",
                            manualStudentError = "No se pudo resolver el estudiante del incidente.",
                            errorMessage = "Selecciona o registra un estudiante valido."
                        )
                    }
                    return@launch
                }
            val student = resolveStudent(current, studentId)
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
                ?: runCatching { saveIncidentUseCase(buildIncidentFromState(current, studentId)) }
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
                ?: buildIncidentFromState(current, studentId).copy(id = incidentId)

            when (val outcome = sendIncidentReportUseCase(student, incident)) {
                is TelegramSendOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedStudentId = studentId,
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
                isProcessingOcr = false,
                detectedOcrText = "",
                ocrSuggestedStudentName = null,
                ocrMatchMessage = null,
                hasManualStudentInputSinceLastOcr = false,
                hasManualTypeSelectionSinceLastOcr = false,
                hasManualSeveritySelectionSinceLastOcr = false,
                hasManualDescriptionEditSinceLastOcr = false,
                showManualStudentForm = false,
                manualStudentDraft = ManualStudentDraft(),
                sendStatus = IncidentSendStatus.Idle,
                studentError = null,
                manualStudentError = null,
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
            current.selectedStudent != null -> null
            !current.showManualStudentForm -> "Selecciona un estudiante o agrégalo manualmente."
            current.manualStudentDraft.fullName.trim().length < MIN_STUDENT_NAME_LENGTH ->
                "Ingresa el nombre completo del estudiante."
            else -> null
        }
        val manualStudentError = if (current.showManualStudentForm && current.manualStudentDraft.fullName.trim().length < MIN_STUDENT_NAME_LENGTH) {
            "El nombre del estudiante debe tener al menos $MIN_STUDENT_NAME_LENGTH caracteres."
        } else {
            null
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
                manualStudentError = manualStudentError,
                typeError = typeError,
                descriptionError = descriptionError,
                errorMessage = listOfNotNull(studentError, manualStudentError, typeError, descriptionError).firstOrNull()
            )
        }
        return studentError == null && manualStudentError == null && typeError == null && descriptionError == null
    }

    private fun buildIncidentFromState(state: IncidentUiState, studentId: Long): Incident = Incident(
        studentId = studentId,
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
                applyOcrAnalysis(
                    rawText = ocr.rawText,
                    isNewOcrRun = true
                )
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isProcessingOcr = false,
                        errorMessage = throwable.message ?: "No se pudo procesar la fotografia del incidente."
                    )
                }
            }
        }
    }

    private fun applyOcrSuggestions() {
        val state = _uiState.value
        if (state.detectedOcrText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "No hay texto OCR para procesar.") }
            return
        }
        applyOcrAnalysis(rawText = state.detectedOcrText, isNewOcrRun = false)
    }

    private fun toggleManualStudentForm(enabled: Boolean) {
        _uiState.update {
            it.copy(
                showManualStudentForm = enabled,
                selectedStudentId = if (enabled) null else it.selectedStudentId,
                hasManualStudentInputSinceLastOcr = true,
                studentError = null,
                manualStudentError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private fun updateManualStudentDraft(update: ManualStudentDraft.() -> ManualStudentDraft) {
        _uiState.update {
            it.copy(
                manualStudentDraft = it.manualStudentDraft.update(),
                hasManualStudentInputSinceLastOcr = true,
                studentError = null,
                manualStudentError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    private suspend fun resolveStudentId(state: IncidentUiState): Long? {
        state.selectedStudent?.let { return it.id }
        if (!state.showManualStudentForm) return null

        val manualName = state.manualStudentDraft.fullName.trim()
        if (manualName.length < MIN_STUDENT_NAME_LENGTH) return null

        val normalizedManualName = normalizeText(manualName)
        state.students.firstOrNull { normalizeText(it.fullName) == normalizedManualName }?.let { existing ->
            return existing.id
        }

        val newStudent = Student(
            id = nextLocalStudentId(state.students),
            fullName = manualName,
            grade = state.manualStudentDraft.grade.trim().ifBlank { "Por definir" },
            section = state.manualStudentDraft.section.trim().ifBlank { "S/N" },
            representativeName = state.manualStudentDraft.representativeName.trim().ifBlank { "Por definir" }
        )
        studentRepository.upsertStudents(listOf(newStudent))
        return newStudent.id
    }

    private fun resolveStudent(state: IncidentUiState, studentId: Long): Student? =
        state.students.firstOrNull { it.id == studentId } ?: run {
            if (!state.showManualStudentForm) {
                null
            } else {
                Student(
                    id = studentId,
                    fullName = state.manualStudentDraft.fullName.trim(),
                    grade = state.manualStudentDraft.grade.trim().ifBlank { "Por definir" },
                    section = state.manualStudentDraft.section.trim().ifBlank { "S/N" },
                    representativeName = state.manualStudentDraft.representativeName.trim().ifBlank { "Por definir" }
                )
            }
        }

    private fun applyOcrAnalysis(rawText: String, isNewOcrRun: Boolean) {
        val state = _uiState.value
        val analysis = analyzeIncidentOcrText(rawText = rawText, students = state.students)

        _uiState.update { current ->
            val shouldUpdateStudent = isNewOcrRun || !current.hasManualStudentInputSinceLastOcr
            val shouldUpdateType = isNewOcrRun || !current.hasManualTypeSelectionSinceLastOcr
            val shouldUpdateSeverity = isNewOcrRun || !current.hasManualSeveritySelectionSinceLastOcr
            val shouldUpdateDescription = isNewOcrRun || !current.hasManualDescriptionEditSinceLastOcr

            val manualStudentDraft = if (shouldUpdateStudent && analysis.detectedStudent == null && !analysis.suggestedStudentName.isNullOrBlank()) {
                current.manualStudentDraft.copy(fullName = analysis.suggestedStudentName)
            } else {
                current.manualStudentDraft
            }

            current.copy(
                isProcessingOcr = false,
                detectedOcrText = rawText,
                selectedStudentId = if (shouldUpdateStudent) analysis.detectedStudent?.id else current.selectedStudentId,
                showManualStudentForm = if (shouldUpdateStudent) {
                    analysis.detectedStudent == null && !analysis.suggestedStudentName.isNullOrBlank()
                } else {
                    current.showManualStudentForm
                },
                manualStudentDraft = manualStudentDraft,
                selectedType = if (shouldUpdateType) {
                    analysis.detectedType ?: current.selectedType ?: IncidentType.OTHER
                } else {
                    current.selectedType
                },
                selectedSeverity = if (shouldUpdateSeverity) {
                    analysis.detectedSeverity ?: current.selectedSeverity
                } else {
                    current.selectedSeverity
                },
                description = if (shouldUpdateDescription) analysis.suggestedDescription else current.description,
                ocrSuggestedStudentName = analysis.suggestedStudentName,
                ocrMatchMessage = buildOcrMatchMessage(analysis),
                hasManualStudentInputSinceLastOcr = if (isNewOcrRun) false else current.hasManualStudentInputSinceLastOcr,
                hasManualTypeSelectionSinceLastOcr = if (isNewOcrRun) false else current.hasManualTypeSelectionSinceLastOcr,
                hasManualSeveritySelectionSinceLastOcr = if (isNewOcrRun) false else current.hasManualSeveritySelectionSinceLastOcr,
                hasManualDescriptionEditSinceLastOcr = if (isNewOcrRun) false else current.hasManualDescriptionEditSinceLastOcr,
                studentError = null,
                manualStudentError = null,
                typeError = null,
                descriptionError = null,
                errorMessage = null,
                successMessage = if (analysis.detectedStudent != null) {
                    "OCR procesado. Campos sugeridos aplicados."
                } else {
                    "OCR procesado. Revisa la sugerencia del estudiante antes de guardar."
                }
            )
        }
    }

    private fun analyzeIncidentOcrText(
        rawText: String,
        students: List<Student>
    ): IncidentOcrAnalysisResult {
        val lines = rawText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        val clauses = splitIntoClauses(rawText)
        val studentHint = extractFieldValue(lines, "estudiante", "alumno", "nombre")
        val studentMatch = findBestStudentMatch(
            lines = lines,
            clauses = clauses,
            studentHint = studentHint,
            students = students
        )
        val suggestedDescription = extractDescription(rawText, lines)
        return IncidentOcrAnalysisResult(
            detectedStudent = studentMatch?.student,
            suggestedStudentName = studentMatch?.student?.fullName ?: studentHint ?: extractLikelyStudentName(lines),
            detectedType = detectIncidentType(rawText, lines),
            detectedSeverity = detectSeverity(rawText, lines),
            suggestedDescription = suggestedDescription,
            confidence = studentMatch?.score
        )
    }

    private fun findBestStudentMatch(
        lines: List<String>,
        clauses: List<String>,
        studentHint: String?,
        students: List<Student>
    ): StudentLineMatch? {
        val sources = buildList {
            studentHint?.takeIf { it.isNotBlank() }?.let(::add)
            addAll(lines)
            addAll(clauses)
        }.distinct()

        return students.mapNotNull { student ->
            val normalizedFullName = normalizeText(student.fullName)
            val aliases = student.aliases.map(::normalizeText).filter { it.isNotBlank() }
            val orderedNameTokens = tokensFromText(normalizedFullName)
            val nameTokens = orderedNameTokens.toSet()

            val bestSource = sources.maxOfOrNull { source ->
                val normalizedSource = normalizeText(source)
                when {
                    normalizedSource.contains(normalizedFullName) -> 1.0
                    aliases.any { alias -> normalizedSource.contains(alias) } -> 0.95
                    nameTokens.size < 2 -> 0.0
                    else -> {
                        val sourceTokens = tokensFromText(normalizedSource).toSet()
                        if (sourceTokens.isEmpty()) {
                            0.0
                        } else {
                            val matched = nameTokens.intersect(sourceTokens).size
                            val dice = (2.0 * matched) / (nameTokens.size + sourceTokens.size).toDouble()
                            val containsFirstName = orderedNameTokens.firstOrNull()?.let(sourceTokens::contains) == true
                            val containsLastName = orderedNameTokens.lastOrNull()?.let(sourceTokens::contains) == true
                            val bonus = if (containsFirstName && containsLastName) 0.15 else 0.0
                            (dice + bonus).coerceAtMost(0.93)
                        }
                    }
                }
            } ?: 0.0

            if (bestSource >= MIN_STUDENT_MATCH_SCORE) {
                StudentLineMatch(student = student, score = bestSource)
            } else {
                null
            }
        }.maxByOrNull { it.score }
    }

    private fun extractLikelyStudentName(lines: List<String>): String? =
        extractFieldValue(lines, "estudiante", "alumno", "nombre")
            ?: lines.firstOrNull { line ->
                val words = tokensFromText(line)
                words.size in 2..5 && line.any(Char::isLetter)
            }

    private fun detectIncidentType(rawText: String, lines: List<String>): IncidentType {
        val explicitType = extractFieldValue(lines, "tipo")
        mapIncidentType(explicitType)?.let { return it }

        val normalizedText = normalizeText(rawText)
        val scores = mapOf(
            IncidentType.BEHAVIOR to countKeywordMatches(
                normalizedText,
                "conducta",
                "comportamiento",
                "indisciplina",
                "pelea",
                "respeto"
            ),
            IncidentType.ACADEMIC to countKeywordMatches(
                normalizedText,
                "nota",
                "rendimiento",
                "tarea",
                "deber",
                "evaluacion",
                "academico"
            ),
            IncidentType.HEALTH to countKeywordMatches(
                normalizedText,
                "dolor",
                "enfermo",
                "salud",
                "accidente",
                "malestar"
            )
        )

        return scores.maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }
            ?.key
            ?: IncidentType.OTHER
    }

    private fun detectSeverity(rawText: String, lines: List<String>): IncidentSeverity? {
        val explicitSeverity = extractFieldValue(lines, "severidad", "gravedad", "nivel")
        mapSeverity(explicitSeverity)?.let { return it }

        val normalizedText = normalizeText(rawText)
        return when {
            containsKeyword(normalizedText, "alta", "grave", "urgente", "critica", "critico") -> IncidentSeverity.HIGH
            containsKeyword(normalizedText, "media", "moderada") -> IncidentSeverity.MEDIUM
            containsKeyword(normalizedText, "baja", "leve") -> IncidentSeverity.LOW
            else -> null
        }
    }

    private fun extractDescription(rawText: String, lines: List<String>): String {
        val descriptionIndex = lines.indexOfFirst { line ->
            val normalizedLine = normalizeText(line)
            containsKeyword(normalizedLine, "descripcion", "detalle")
        }
        if (descriptionIndex == -1) return rawText.trim()

        val descriptionLines = mutableListOf<String>()
        val firstLine = lines[descriptionIndex]
        val remainder = firstLine.substringAfter(':', "").substringAfter('-', "").trim()
        if (remainder.isNotBlank()) {
            descriptionLines += remainder
        }
        lines.drop(descriptionIndex + 1)
            .takeWhile { line -> !isStructuredFieldLine(line) }
            .forEach(descriptionLines::add)

        return descriptionLines.joinToString(" ").ifBlank { rawText.trim() }
    }

    private fun extractFieldValue(lines: List<String>, vararg labels: String): String? {
        val normalizedLabels = labels.map(::normalizeText)
        return lines.firstNotNullOfOrNull { line ->
            val normalizedLine = normalizeText(line)
            val label = normalizedLabels.firstOrNull { current ->
                normalizedLine.startsWith("$current ") || normalizedLine.startsWith("$current:")
            } ?: return@firstNotNullOfOrNull null

            line.substringAfter(':', "")
                .ifBlank { line.substringAfter(' ', "") }
                .trim()
                .ifBlank { null }
        }
    }

    private fun mapIncidentType(value: String?): IncidentType? {
        val normalized = value?.let(::normalizeText).orEmpty()
        return when {
            containsKeyword(normalized, "conducta", "comportamiento", "indisciplina", "pelea", "respeto") -> IncidentType.BEHAVIOR
            containsKeyword(normalized, "academico", "nota", "rendimiento", "tarea", "deber", "evaluacion") -> IncidentType.ACADEMIC
            containsKeyword(normalized, "salud", "dolor", "enfermo", "accidente", "malestar") -> IncidentType.HEALTH
            normalized.isNotBlank() -> IncidentType.OTHER
            else -> null
        }
    }

    private fun mapSeverity(value: String?): IncidentSeverity? {
        val normalized = value?.let(::normalizeText).orEmpty()
        return when {
            containsKeyword(normalized, "alta", "grave", "urgente", "critica", "critico") -> IncidentSeverity.HIGH
            containsKeyword(normalized, "media", "moderada") -> IncidentSeverity.MEDIUM
            containsKeyword(normalized, "baja", "leve") -> IncidentSeverity.LOW
            else -> null
        }
    }

    private fun buildOcrMatchMessage(analysis: IncidentOcrAnalysisResult): String =
        analysis.detectedStudent?.let { student ->
            val percentage = ((analysis.confidence ?: 0.0) * 100).toInt()
            "Estudiante detectado: ${student.fullName} (${percentage}% de coincidencia)."
        } ?: analysis.suggestedStudentName?.let { name ->
            "No se pudo identificar automaticamente al estudiante. Sugerencia OCR: $name."
        } ?: "No se pudo identificar automaticamente al estudiante."

    private fun splitIntoClauses(text: String): List<String> =
        text
            .split('\n', ',', ';', '.')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun tokensFromText(value: String): List<String> =
        normalizeText(value)
            .split(" ")
            .filter { token -> token.length >= MIN_TOKEN_LENGTH }

    private fun countKeywordMatches(text: String, vararg keywords: String): Int =
        keywords.count { keyword -> containsWordSequence(text, normalizeText(keyword)) }

    private fun containsKeyword(text: String, vararg keywords: String): Boolean =
        keywords.any { keyword -> containsWordSequence(text, normalizeText(keyword)) }

    private fun containsWordSequence(text: String, candidate: String): Boolean =
        Regex("(^|\\s)${Regex.escape(candidate)}($|\\s)").containsMatchIn(text)

    private fun isStructuredFieldLine(line: String): Boolean {
        val normalizedLine = normalizeText(line)
        return listOf("estudiante", "alumno", "nombre", "tipo", "severidad", "gravedad", "nivel")
            .any { label -> normalizedLine.startsWith("$label ") || normalizedLine.startsWith("$label:") }
    }

    private fun normalizeText(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

    private fun nextLocalStudentId(students: List<Student>): Long =
        (students.maxOfOrNull { it.id } ?: 0L) + 1L

    companion object {
        private const val MIN_DESCRIPTION_LENGTH = 10
        private const val MIN_STUDENT_NAME_LENGTH = 5
        private const val MIN_STUDENT_MATCH_SCORE = 0.78
        private const val MIN_TOKEN_LENGTH = 3
    }
}

private data class StudentLineMatch(
    val student: Student,
    val score: Double
)

private data class IncidentOcrAnalysisResult(
    val detectedStudent: Student?,
    val suggestedStudentName: String?,
    val detectedType: IncidentType?,
    val detectedSeverity: IncidentSeverity?,
    val suggestedDescription: String,
    val confidence: Double?
)
