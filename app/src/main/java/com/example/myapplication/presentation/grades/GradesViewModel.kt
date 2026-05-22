package com.example.myapplication.presentation.grades

import android.net.Uri
import com.example.myapplication.BuildConfig
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.domain.model.sync.SyncOutcome
import com.example.myapplication.domain.repository.SyncRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.usecase.grade.GetGradesBySubjectAndPeriodUseCase
import com.example.myapplication.domain.usecase.grade.SaveGradeUseCase
import com.example.myapplication.domain.usecase.ocr.RecognizeTextFromImageUseCase
import com.example.myapplication.domain.usecase.student.GetStudentsUseCase
import com.example.myapplication.domain.model.telegram.TelegramSendOutcome
import com.example.myapplication.domain.usecase.telegram.SendTelegramMessageUseCase
import com.example.myapplication.presentation.grades.components.GradeStudentMock
import com.example.myapplication.presentation.grades.components.GradeVisualStatus
import com.example.myapplication.services.telegram.TelegramMessageBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GradesViewModel(
    private val getStudentsUseCase: GetStudentsUseCase,
    private val getGradesBySubjectAndPeriodUseCase: GetGradesBySubjectAndPeriodUseCase,
    private val saveGradeUseCase: SaveGradeUseCase,
    private val recognizeTextFromImageUseCase: RecognizeTextFromImageUseCase,
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase,
    private val networkMonitor: NetworkMonitor? = null,
    private val syncRepository: SyncRepository? = null,
    initialState: GradesUiState = GradesUiState()
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    private var gradeStore: List<Grade> = emptyList()
    private var pendingEdits: Map<Long, GradeEditDraft> = emptyMap()
    private var hasPendingLocalEdits = false

    init {
        networkMonitor?.let { monitor ->
            viewModelScope.launch {
                monitor.isOnline.collect { online ->
                    _uiState.update { it.copy(isOffline = !online) }
                }
            }
        }
        viewModelScope.launch {
            getStudentsUseCase().collect { students ->
                val defaultSubject = _uiState.value.selectedSubject.takeIf { it.isNotBlank() } ?: "General"
                val defaultPeriod = _uiState.value.selectedPeriod.takeIf { it.isNotBlank() } ?: "Actual"
                _uiState.update {
                    it.copy(
                        studentsDomain = students,
                        students = studentsFromDomain(students),
                        courseName = students.firstOrNull()?.let { student ->
                            "${student.grade} Grado Sección ${student.section}"
                        } ?: "Curso no asignado",
                        subjects = listOf(defaultSubject),
                        periods = listOf(defaultPeriod),
                        selectedSubject = defaultSubject,
                        selectedPeriod = defaultPeriod,
                        isLoading = false
                    )
                }
                observeGrades(defaultSubject, defaultPeriod)
            }
        }
    }

    fun onEvent(event: GradesEvent) {
        when (event) {
            GradesEvent.LoadData -> loadData()
            is GradesEvent.SubjectSelected -> {
                clearPendingEdits()
                _uiState.update { it.copy(selectedSubject = event.subject) }
                observeGrades(event.subject, _uiState.value.selectedPeriod)
            }

            is GradesEvent.PeriodSelected -> {
                clearPendingEdits()
                _uiState.update { it.copy(selectedPeriod = event.period) }
                observeGrades(_uiState.value.selectedSubject, event.period)
            }

            is GradesEvent.EditGrade -> {
                applyPendingEdit(
                    studentId = event.studentId,
                    score = event.score,
                    maxScore = event.maxScore,
                    activityName = event.activityName,
                    activityType = event.activityType
                )
            }

            is GradesEvent.OpenEditDialog -> {
                _uiState.update { it.copy(editingStudentId = event.studentId) }
            }

            GradesEvent.DismissEditDialog -> {
                _uiState.update { it.copy(editingStudentId = null) }
            }

            GradesEvent.RefreshAverages -> refreshDerivedMetrics()
            is GradesEvent.OpenDetail -> Unit
            is GradesEvent.OcrImageSelected -> processOcr(event.uri)
            GradesEvent.ApplyOcrSuggestions -> applyOcrSuggestions()
            GradesEvent.SaveGrades -> saveGrades()
            GradesEvent.SendBulletinClicked -> sendBulletin()
            GradesEvent.ClearMessages -> {
                _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            }
        }
    }

    private fun loadData() {
        val state = _uiState.value
        observeGrades(state.selectedSubject, state.selectedPeriod)
    }

    private fun observeGrades(subject: String, period: String) {
        if (subject.isBlank() || period.isBlank()) return
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            combine(
                getStudentsUseCase(),
                getGradesBySubjectAndPeriodUseCase(subject, period)
            ) { students, grades ->
                students to grades
            }.collect { (students, grades) ->
                if (!hasPendingLocalEdits) {
                    gradeStore = grades
                }
                val subjects = (grades.map { it.subject } + subject).distinct()
                val periods = (grades.map { it.period } + period).distinct()
                _uiState.update {
                    it.copy(
                        studentsDomain = students,
                        students = studentsFromDomain(students),
                        gradesDomain = if (hasPendingLocalEdits) it.gradesDomain else grades,
                        subjects = subjects,
                        periods = periods,
                        selectedSubject = subject,
                        selectedPeriod = period,
                        isLoading = false
                    )
                }
                refreshDerivedMetrics()
            }
        }
    }

    private fun applyPendingEdit(
        studentId: Long,
        score: Double,
        maxScore: Double,
        activityName: String,
        activityType: String
    ) {
        val normalizedScore = score.coerceIn(0.0, maxScore)
        hasPendingLocalEdits = true
        pendingEdits = pendingEdits + (
            studentId to GradeEditDraft(
                studentId = studentId,
                activityName = activityName,
                activityType = activityType,
                score = normalizedScore,
                maxScore = maxScore
            )
            )
        _uiState.update {
            it.copy(
                editingStudentId = null,
                hasUnsavedEdits = true,
                errorMessage = null,
                successMessage = null
            )
        }
        refreshDerivedMetrics()
    }

    private fun refreshDerivedMetrics() {
        val state = _uiState.value
        val filtered = gradeStore.filter {
            it.subject == state.selectedSubject && it.period == state.selectedPeriod
        }
        val gradesByStudent = filtered
            .groupBy { it.studentId }
            .mapValues { (_, values) -> values.map { it.score }.average().takeIf { !it.isNaN() } ?: 0.0 }
            .toMutableMap()

        pendingEdits.values.forEach { draft ->
            gradesByStudent[draft.studentId] = draft.score
        }

        val classAverage = gradesByStudent.values.average().takeIf { !it.isNaN() } ?: 0.0
        val maxScore = pendingEdits.values.firstOrNull()?.maxScore
            ?: filtered.firstOrNull()?.maxScore
            ?: GradeEditDraft.DEFAULT_MAX_SCORE

        val updatedStudents = state.students.map { student ->
            val score = gradesByStudent[student.id]
            student.copy(
                score = score?.toInt(),
                maxScore = maxScore.toInt(),
                status = when {
                    score == null -> GradeVisualStatus.NOT_REGISTERED
                    score < 10.0 -> GradeVisualStatus.AT_RISK
                    else -> GradeVisualStatus.APPROVED
                }
            )
        }

        _uiState.update {
            it.copy(
                gradesDomain = filtered,
                grades = gradesByStudent,
                students = updatedStudents,
                classAverage = classAverage,
                approvedCount = updatedStudents.count { student -> student.status == GradeVisualStatus.APPROVED },
                riskCount = updatedStudents.count { student -> student.status == GradeVisualStatus.AT_RISK },
                activitiesCount = filtered.map { grade -> grade.activityName }.distinct().size
            )
        }
    }

    private fun saveGrades() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            if (pendingEdits.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "No hay calificaciones pendientes por guardar."
                    )
                }
                return@launch
            }

            val subject = _uiState.value.selectedSubject
            val period = _uiState.value.selectedPeriod

            runCatching {
                pendingEdits.values.forEach { draft ->
                    saveGradeUseCase(
                        Grade(
                            studentId = draft.studentId,
                            subject = subject,
                            period = period,
                            activityName = draft.activityName,
                            activityType = draft.activityType,
                            score = draft.score,
                            maxScore = draft.maxScore,
                            syncPending = true
                        )
                    )
                }
            }.onSuccess {
                hasPendingLocalEdits = false
                pendingEdits = emptyMap()
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
                        hasUnsavedEdits = false,
                        successMessage = "Calificaciones guardadas correctamente.$syncMessage"
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "No se pudieron guardar las calificaciones."
                    )
                }
            }
        }
    }

    private fun sendBulletin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null, successMessage = null) }
            val state = _uiState.value
            if (state.hasUnsavedEdits) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = "Guarda las calificaciones antes de enviar el boletín."
                    )
                }
                return@launch
            }

            val filteredGrades = state.gradesDomain.filter {
                it.subject == state.selectedSubject && it.period == state.selectedPeriod
            }
            val gradesByStudent = filteredGrades.groupBy { it.studentId }
            val reportRecords = state.studentsDomain.mapNotNull { student ->
                val studentGrades = gradesByStudent[student.id] ?: return@mapNotNull null
                val average = studentGrades.map { it.score }.average()
                val maxScore = studentGrades.maxOf { it.maxScore }
                TelegramMessageBuilder.GradeReportRecord(
                    student = student,
                    averageScore = average,
                    maxScore = maxScore
                )
            }

            if (reportRecords.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = "No hay calificaciones para enviar en el filtro actual."
                    )
                }
                return@launch
            }

            val message = TelegramMessageBuilder.buildGradeReport(
                subject = state.selectedSubject,
                period = state.selectedPeriod,
                records = reportRecords
            )
            when (val outcome = sendTelegramMessageUseCase(BuildConfig.TELEGRAM_DEFAULT_CHAT_ID, message)) {
                is TelegramSendOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            successMessage = "Boletín enviado por Telegram.",
                            errorMessage = null
                        )
                    }
                }
                is TelegramSendOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            successMessage = null,
                            errorMessage = outcome.message
                        )
                    }
                }
            }
        }
    }

    private fun processOcr(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true, errorMessage = null, successMessage = null) }
            val result = recognizeTextFromImageUseCase(uri)
            result.onSuccess { ocr ->
                _uiState.update {
                    it.copy(
                        isProcessingOcr = false,
                        detectedOcrText = ocr.rawText,
                        successMessage = "OCR completado. Revisa y pulsa Aplicar sugerencias."
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isProcessingOcr = false,
                        errorMessage = throwable.message ?: "No se pudo procesar la imagen de calificaciones."
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

        val lines = state.detectedOcrText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val updates = state.students.mapNotNull { student ->
            inferScoreFromOcr(student.name, lines)?.let { score ->
                student.id to GradeEditDraft(
                    studentId = student.id,
                    score = score.coerceIn(0.0, GradeEditDraft.DEFAULT_MAX_SCORE),
                    maxScore = GradeEditDraft.DEFAULT_MAX_SCORE
                )
            }
        }.toMap()

        if (updates.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "No se detectaron notas en el texto OCR. Edita manualmente.")
            }
            return
        }

        hasPendingLocalEdits = true
        pendingEdits = pendingEdits + updates
        _uiState.update {
            it.copy(
                hasUnsavedEdits = true,
                successMessage = "Sugerencias OCR aplicadas. Verifica y guarda."
            )
        }
        refreshDerivedMetrics()
    }

    private fun inferScoreFromOcr(studentName: String, lines: List<String>): Double? {
        val studentKey = studentName.lowercase()
        val line = lines.firstOrNull { it.lowercase().contains(studentKey) } ?: return null
        val number = Regex("""(\d+(?:[.,]\d+)?)""").findAll(line).map { it.value.replace(',', '.') }.lastOrNull()
        return number?.toDoubleOrNull()
    }

    private fun clearPendingEdits() {
        hasPendingLocalEdits = false
        pendingEdits = emptyMap()
        _uiState.update { it.copy(hasUnsavedEdits = false, editingStudentId = null) }
    }

    private fun studentsFromDomain(students: List<Student>): List<GradeStudentMock> =
        students.map { student ->
            GradeStudentMock(
                id = student.id,
                name = student.fullName,
                score = null,
                status = GradeVisualStatus.NOT_REGISTERED
            )
        }
}
