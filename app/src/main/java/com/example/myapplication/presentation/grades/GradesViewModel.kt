package com.example.myapplication.presentation.grades

import android.net.Uri
import com.example.myapplication.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.model.Grade
import com.example.myapplication.presentation.grades.components.GradeStudentMock
import com.example.myapplication.presentation.grades.components.GradeVisualStatus
import com.example.myapplication.domain.usecase.grade.GetGradesBySubjectAndPeriodUseCase
import com.example.myapplication.domain.usecase.grade.SaveGradeUseCase
import com.example.myapplication.domain.usecase.ocr.RecognizeTextFromImageUseCase
import com.example.myapplication.domain.usecase.student.GetStudentsUseCase
import com.example.myapplication.domain.usecase.telegram.SendTelegramMessageUseCase
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
    initialState: GradesUiState = GradesUiState()
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    private var gradeStore: List<Grade> = emptyList()

    init {
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
                _uiState.update { it.copy(selectedSubject = event.subject) }
                observeGrades(event.subject, _uiState.value.selectedPeriod)
            }

            is GradesEvent.PeriodSelected -> {
                _uiState.update { it.copy(selectedPeriod = event.period) }
                observeGrades(_uiState.value.selectedSubject, event.period)
            }

            is GradesEvent.EditGrade -> {
                updateGrade(
                    studentId = event.studentId,
                    activityName = event.activityName,
                    activityType = event.activityType,
                    score = event.score,
                    maxScore = event.maxScore
                )
            }

            GradesEvent.RefreshAverages -> refreshDerivedMetrics()
            is GradesEvent.OpenDetail -> Unit
            is GradesEvent.OcrImageSelected -> processOcr(event.uri)
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
                gradeStore = grades
                val subjects = (grades.map { it.subject } + subject).distinct()
                val periods = (grades.map { it.period } + period).distinct()
                _uiState.update {
                    it.copy(
                        studentsDomain = students,
                        students = studentsFromDomain(students),
                        gradesDomain = grades,
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

    private fun updateGrade(
        studentId: Long,
        activityName: String,
        activityType: String,
        score: Double,
        maxScore: Double
    ) {
        val normalizedScore = score.coerceIn(0.0, maxScore)
        val subject = _uiState.value.selectedSubject
        val period = _uiState.value.selectedPeriod

        viewModelScope.launch {
            runCatching {
                saveGradeUseCase(
                    Grade(
                        studentId = studentId,
                        subject = subject,
                        period = period,
                        activityName = activityName,
                        activityType = activityType,
                        score = normalizedScore,
                        maxScore = maxScore,
                        syncPending = true
                    )
                )
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(errorMessage = throwable.message ?: "No se pudo guardar la calificación.")
                }
            }
        }
    }

    private fun refreshDerivedMetrics() {
        val state = _uiState.value
        val filtered = gradeStore.filter {
            it.subject == state.selectedSubject && it.period == state.selectedPeriod
        }
        val gradesByStudent = filtered
            .groupBy { it.studentId }
            .mapValues { (_, values) -> values.map { it.score }.average().takeIf { !it.isNaN() } ?: 0.0 }
        val classAverage = gradesByStudent.values.average().takeIf { !it.isNaN() } ?: 0.0

        val updatedStudents = state.students.map { student ->
            val score = gradesByStudent[student.id]
            student.copy(
                score = score?.toInt(),
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
            _uiState.update {
                it.copy(
                    isSaving = false,
                    successMessage = "Calificaciones actualizadas en almacenamiento local."
                )
            }
        }
    }

    private fun sendBulletin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null, successMessage = null) }
            val state = _uiState.value
            val filteredGrades = state.gradesDomain.filter {
                it.subject == state.selectedSubject && it.period == state.selectedPeriod
            }
            val gradesByStudent = filteredGrades.groupBy { it.studentId }
            val reportRecords = state.studentsDomain.mapNotNull { student ->
                val grade = gradesByStudent[student.id]
                    ?.firstOrNull()
                    ?.copy(score = gradesByStudent[student.id]!!.map { it.score }.average())
                    ?: return@mapNotNull null
                student to grade
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
            val sent = sendTelegramMessageUseCase(BuildConfig.TELEGRAM_DEFAULT_CHAT_ID, message)
            _uiState.update {
                it.copy(
                    isSending = false,
                    successMessage = if (sent) "Boletín enviado por Telegram."
                    else "Boletín preparado, pero no se pudo enviar por Telegram.",
                    errorMessage = if (!sent) {
                        "Verifica TELEGRAM_BOT_TOKEN y TELEGRAM_CHAT_ID en local.properties."
                    } else null
                )
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
                        successMessage = "OCR completado. Revisa texto detectado para registrar notas."
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

    private fun studentsFromDomain(students: List<com.example.myapplication.domain.model.Student>): List<GradeStudentMock> =
        students.map { student ->
            GradeStudentMock(
                id = student.id,
                name = student.fullName,
                score = null,
                status = GradeVisualStatus.NOT_REGISTERED
            )
        }
}
