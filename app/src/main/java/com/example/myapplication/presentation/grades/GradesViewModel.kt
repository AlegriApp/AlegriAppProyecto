package com.example.myapplication.presentation.grades

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.model.sync.SyncOutcome
import com.example.myapplication.domain.model.telegram.TelegramSendOutcome
import com.example.myapplication.domain.repository.CatalogRepository
import com.example.myapplication.domain.repository.SyncRepository
import com.example.myapplication.domain.usecase.grade.GetGradesByCatalogFiltersUseCase
import com.example.myapplication.domain.usecase.grade.SaveGradeUseCase
import com.example.myapplication.domain.usecase.ocr.RecognizeTextFromImageUseCase
import com.example.myapplication.domain.usecase.student.GetStudentsByCourseUseCase
import com.example.myapplication.domain.usecase.telegram.SendParentTelegramUseCase
import com.example.myapplication.presentation.common.CatalogOption
import com.example.myapplication.presentation.grades.components.GradeStudentMock
import com.example.myapplication.presentation.grades.components.GradeVisualStatus
import com.example.myapplication.services.telegram.TelegramMessageBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GradesViewModel(
    private val getStudentsByCourseUseCase: GetStudentsByCourseUseCase,
    private val getGradesByCatalogFiltersUseCase: GetGradesByCatalogFiltersUseCase,
    private val catalogRepository: CatalogRepository,
    private val saveGradeUseCase: SaveGradeUseCase,
    private val recognizeTextFromImageUseCase: RecognizeTextFromImageUseCase,
    private val sendParentTelegramUseCase: SendParentTelegramUseCase,
    private val networkMonitor: NetworkMonitor? = null,
    private val syncRepository: SyncRepository? = null,
    initialState: GradesUiState = GradesUiState()
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null
    private var subjectsJob: Job? = null

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
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                syncRepository?.syncAll()
                catalogRepository.syncCatalogsFromRemote()
            }
        }
        viewModelScope.launch {
            combine(
                catalogRepository.observeCourses(),
                catalogRepository.observeEvaluationTypes(),
                catalogRepository.observeAcademicPeriods()
            ) { courses, evalTypes, periods ->
                Triple(courses, evalTypes, periods)
            }.collect { (courses, evalTypes, periods) ->
                val courseOptions = courses.map { CatalogOption(it.id, it.displayName) }
                val evalOptions = evalTypes.map { CatalogOption(it.id, it.nombre) }
                val periodOptions = periods.map { CatalogOption(it.id, it.nombre) }
                val selectedCourse = _uiState.value.selectedCourseId ?: courseOptions.firstOrNull()?.id
                val selectedEval = _uiState.value.selectedEvaluationTypeId ?: evalOptions.firstOrNull()?.id
                val selectedPeriod = _uiState.value.selectedPeriodId ?: periodOptions.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        courseOptions = courseOptions,
                        evaluationTypeOptions = evalOptions,
                        periodOptions = periodOptions,
                        selectedCourseId = selectedCourse,
                        selectedEvaluationTypeId = selectedEval,
                        selectedPeriodId = selectedPeriod,
                        courseName = courseOptions.firstOrNull { o -> o.id == selectedCourse }?.label.orEmpty()
                    )
                }
                selectedCourse?.let { loadSubjectsForCourse(it) }
                val subjectId = _uiState.value.selectedSubjectId
                if (selectedCourse != null && subjectId != null && selectedEval != null && selectedPeriod != null) {
                    observeGrades(selectedCourse, subjectId, selectedEval, selectedPeriod)
                }
            }
        }
    }

    fun onEvent(event: GradesEvent) {
        when (event) {
            GradesEvent.LoadData -> loadData()
            is GradesEvent.CourseSelected -> {
                clearPendingEdits()
                val label = _uiState.value.courseOptions.firstOrNull { it.id == event.courseId }?.label.orEmpty()
                _uiState.update {
                    it.copy(
                        selectedCourseId = event.courseId,
                        courseName = label,
                        selectedSubjectId = null,
                        subjectOptions = emptyList()
                    )
                }
                loadSubjectsForCourse(event.courseId)
                restartObserveIfReady()
            }
            is GradesEvent.SubjectSelected -> {
                clearPendingEdits()
                val label = _uiState.value.subjectOptions.firstOrNull { it.id == event.subjectId }?.label.orEmpty()
                _uiState.update { it.copy(selectedSubjectId = event.subjectId, selectedSubject = label) }
                restartObserveIfReady()
            }
            is GradesEvent.EvaluationTypeSelected -> {
                clearPendingEdits()
                _uiState.update { it.copy(selectedEvaluationTypeId = event.evaluationTypeId) }
                restartObserveIfReady()
            }
            is GradesEvent.PeriodSelected -> {
                clearPendingEdits()
                val label = _uiState.value.periodOptions.firstOrNull { it.id == event.periodId }?.label.orEmpty()
                _uiState.update { it.copy(selectedPeriodId = event.periodId, selectedPeriod = label) }
                restartObserveIfReady()
            }
            is GradesEvent.EditGrade -> applyPendingEdit(
                studentId = event.studentId,
                score = event.score,
                maxScore = event.maxScore,
                activityName = event.activityName,
                activityType = event.activityType
            )
            is GradesEvent.OpenEditDialog -> _uiState.update { it.copy(editingStudentId = event.studentId) }
            GradesEvent.DismissEditDialog -> _uiState.update { it.copy(editingStudentId = null) }
            GradesEvent.RefreshAverages -> refreshDerivedMetrics()
            is GradesEvent.OpenDetail -> Unit
            is GradesEvent.OcrImageSelected -> processOcr(event.uri)
            GradesEvent.ApplyOcrSuggestions -> applyOcrSuggestions()
            GradesEvent.SaveGrades -> saveGrades()
            GradesEvent.SendBulletinClicked -> sendBulletin()
            GradesEvent.ClearMessages -> _uiState.update { it.copy(errorMessage = null, successMessage = null) }
        }
    }

    private fun loadSubjectsForCourse(courseId: Long) {
        subjectsJob?.cancel()
        subjectsJob = viewModelScope.launch {
            catalogRepository.observeSubjectsByCourse(courseId).collect { subjects ->
                val options = subjects.map { CatalogOption(it.id, it.nombre) }
                val selected = _uiState.value.selectedSubjectId ?: options.firstOrNull()?.id
                _uiState.update {
                    it.copy(
                        subjectOptions = options,
                        selectedSubjectId = selected,
                        selectedSubject = options.firstOrNull { o -> o.id == selected }?.label.orEmpty()
                    )
                }
                restartObserveIfReady()
            }
        }
    }

    private fun restartObserveIfReady() {
        val s = _uiState.value
        val courseId = s.selectedCourseId ?: return
        val subjectId = s.selectedSubjectId ?: return
        val evalId = s.selectedEvaluationTypeId ?: return
        val periodId = s.selectedPeriodId ?: return
        observeGrades(courseId, subjectId, evalId, periodId)
    }

    private fun loadData() = restartObserveIfReady()

    private fun observeGrades(courseId: Long, subjectId: Long, evaluationTypeId: Long, periodId: Long) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            combine(
                getStudentsByCourseUseCase(courseId),
                getGradesByCatalogFiltersUseCase(courseId, subjectId, evaluationTypeId, periodId)
            ) { students, grades -> students to grades }
                .collect { (students, grades) ->
                    if (!hasPendingLocalEdits) gradeStore = grades
                    _uiState.update {
                        it.copy(
                            studentsDomain = students,
                            students = studentsFromDomain(students),
                            gradesDomain = if (hasPendingLocalEdits) it.gradesDomain else grades,
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
        hasPendingLocalEdits = true
        pendingEdits = pendingEdits + (
            studentId to GradeEditDraft(
                studentId = studentId,
                activityName = activityName,
                activityType = activityType,
                score = score.coerceIn(0.0, maxScore),
                maxScore = maxScore
            )
            )
        _uiState.update {
            it.copy(editingStudentId = null, hasUnsavedEdits = true, errorMessage = null, successMessage = null)
        }
        refreshDerivedMetrics()
    }

    private fun refreshDerivedMetrics() {
        val state = _uiState.value
        val gradesByStudent = gradeStore
            .groupBy { it.studentId }
            .mapValues { (_, values) -> values.map { it.score }.average().takeIf { !it.isNaN() } ?: 0.0 }
            .toMutableMap()
        pendingEdits.values.forEach { draft -> gradesByStudent[draft.studentId] = draft.score }
        val maxScore = pendingEdits.values.firstOrNull()?.maxScore
            ?: gradeStore.firstOrNull()?.maxScore
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
                gradesDomain = gradeStore,
                grades = gradesByStudent,
                students = updatedStudents,
                classAverage = gradesByStudent.values.average().takeIf { avg -> !avg.isNaN() } ?: 0.0,
                approvedCount = updatedStudents.count { s -> s.status == GradeVisualStatus.APPROVED },
                riskCount = updatedStudents.count { s -> s.status == GradeVisualStatus.AT_RISK },
                activitiesCount = gradeStore.map { g -> g.activityName }.distinct().size
            )
        }
    }

    private fun saveGrades() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            val state = _uiState.value
            val courseId = state.selectedCourseId
            val subjectId = state.selectedSubjectId
            val evalId = state.selectedEvaluationTypeId
            val periodId = state.selectedPeriodId
            if (courseId == null || subjectId == null || evalId == null || periodId == null) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Completa curso, materia, tipo y periodo.") }
                return@launch
            }
            val drafts = collectGradesToSave(state)
            if (drafts.isEmpty()) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Ingresa al menos una calificación antes de guardar.")
                }
                return@launch
            }
            val evalLabel = state.evaluationTypeOptions.firstOrNull { it.id == evalId }?.label ?: "Evaluación"
            runCatching {
                val saved = mutableListOf<Grade>()
                drafts.forEach { draft ->
                    val grade = Grade(
                        studentId = draft.studentId,
                        courseId = courseId,
                        subjectId = subjectId,
                        periodAcademicId = periodId,
                        evaluationTypeId = evalId,
                        subject = state.selectedSubject,
                        period = state.selectedPeriod,
                        activityName = draft.activityName.ifBlank { state.selectedSubject },
                        activityType = draft.activityType.ifBlank { evalLabel },
                        score = draft.score,
                        maxScore = draft.maxScore,
                        syncPending = true
                    )
                    saveGradeUseCase(grade)
                    saved += grade
                }
                saved
            }.onSuccess { saved ->
                hasPendingLocalEdits = false
                pendingEdits = emptyMap()
                gradeStore = saved
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
                        gradesDomain = saved,
                        successMessage = "Calificaciones guardadas en el dispositivo (${saved.size}).$syncMessage"
                    )
                }
                refreshDerivedMetrics()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = throwable.message ?: "No se pudieron guardar las calificaciones.")
                }
            }
        }
    }

    private fun collectGradesToSave(state: GradesUiState): List<GradeEditDraft> {
        if (pendingEdits.isNotEmpty()) return pendingEdits.values.toList()
        val evalLabel = state.evaluationTypeOptions
            .firstOrNull { it.id == state.selectedEvaluationTypeId }
            ?.label
            ?: GradeEditDraft.DEFAULT_ACTIVITY_TYPE
        return state.students.mapNotNull { student ->
            val score = student.score?.toDouble() ?: return@mapNotNull null
            GradeEditDraft(
                studentId = student.id,
                activityName = state.selectedSubject.ifBlank { GradeEditDraft.DEFAULT_ACTIVITY_NAME },
                activityType = evalLabel,
                score = score,
                maxScore = student.maxScore.toDouble()
            )
        }
    }

    private fun sendBulletin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null, successMessage = null) }
            val state = _uiState.value
            if (state.hasUnsavedEdits) {
                _uiState.update { it.copy(isSending = false, errorMessage = "Guarda las calificaciones antes de enviar el boletín.") }
                return@launch
            }
            val gradesByStudent = state.gradesDomain.groupBy { it.studentId }
            if (gradesByStudent.isEmpty()) {
                _uiState.update { it.copy(isSending = false, errorMessage = "No hay calificaciones guardadas para enviar.") }
                return@launch
            }
            var sent = 0
            var failed = 0
            val errors = mutableListOf<String>()
            state.studentsDomain.forEach { student ->
                val studentGrades = gradesByStudent[student.id] ?: return@forEach
                val lines = studentGrades.map { g ->
                    TelegramMessageBuilder.GradeActivityLine(
                        activityName = g.activityName,
                        activityType = g.activityType,
                        score = g.score,
                        maxScore = g.maxScore
                    )
                }
                val message = TelegramMessageBuilder.buildGradeReportForParent(
                    student = student,
                    subject = state.selectedSubject,
                    period = state.selectedPeriod,
                    courseName = state.courseName,
                    lines = lines
                )
                when (val outcome = sendParentTelegramUseCase(student, message)) {
                    is TelegramSendOutcome.Success -> sent++
                    is TelegramSendOutcome.Failure -> {
                        failed++
                        errors += "${student.fullName}: ${outcome.message}"
                    }
                }
            }
            val summary = when {
                sent > 0 && failed == 0 -> "Boletín enviado a $sent representante(s) por Telegram."
                sent > 0 -> "Enviado a $sent. Fallos: $failed. ${errors.firstOrNull().orEmpty()}"
                else -> errors.firstOrNull() ?: "No se pudo enviar a ningún representante."
            }
            _uiState.update {
                it.copy(
                    isSending = false,
                    successMessage = if (sent > 0) summary else null,
                    errorMessage = if (sent == 0) summary else if (failed > 0) summary else null
                )
            }
        }
    }

    private fun processOcr(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOcr = true, errorMessage = null, successMessage = null) }
            recognizeTextFromImageUseCase(uri)
                .onSuccess { ocr ->
                    _uiState.update {
                        it.copy(isProcessingOcr = false, detectedOcrText = ocr.rawText, successMessage = "OCR completado. Revisa y pulsa Aplicar sugerencias.")
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isProcessingOcr = false, errorMessage = throwable.message ?: "No se pudo procesar la imagen de calificaciones.")
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
        val lines = state.detectedOcrText.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        val evalLabel = state.evaluationTypeOptions.firstOrNull { it.id == state.selectedEvaluationTypeId }?.label ?: "Evaluación"
        val updates = state.students.mapNotNull { student ->
            inferScoreFromOcr(student.name, lines)?.let { score ->
                student.id to GradeEditDraft(
                    studentId = student.id,
                    activityName = evalLabel,
                    activityType = evalLabel,
                    score = score.coerceIn(0.0, GradeEditDraft.DEFAULT_MAX_SCORE),
                    maxScore = GradeEditDraft.DEFAULT_MAX_SCORE
                )
            }
        }.toMap()
        if (updates.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No se detectaron notas en el texto OCR. Edita manualmente.") }
            return
        }
        hasPendingLocalEdits = true
        pendingEdits = pendingEdits + updates
        _uiState.update { it.copy(hasUnsavedEdits = true, successMessage = "Sugerencias OCR aplicadas. Verifica y guarda.") }
        refreshDerivedMetrics()
    }

    private fun inferScoreFromOcr(studentName: String, lines: List<String>): Double? {
        val line = lines.firstOrNull { it.lowercase().contains(studentName.lowercase()) } ?: return null
        return Regex("""(\d+(?:[.,]\d+)?)""").findAll(line).map { it.value.replace(',', '.') }.lastOrNull()?.toDoubleOrNull()
    }

    private fun clearPendingEdits() {
        hasPendingLocalEdits = false
        pendingEdits = emptyMap()
        _uiState.update { it.copy(hasUnsavedEdits = false, editingStudentId = null) }
    }

    private fun studentsFromDomain(students: List<Student>): List<GradeStudentMock> =
        students.map { student ->
            GradeStudentMock(id = student.id, name = student.fullName, score = null, status = GradeVisualStatus.NOT_REGISTERED)
        }
}
