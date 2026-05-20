package com.example.myapplication.presentation.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.model.Student
import com.example.myapplication.presentation.grades.components.GradeStudentMock
import com.example.myapplication.presentation.grades.components.GradeVisualStatus
import com.example.myapplication.presentation.grades.components.gradesMockStudents
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GradesViewModel(
    initialState: GradesUiState = GradesUiState()
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<GradesUiState> = _uiState.asStateFlow()

    private val mockStudents = listOf(
        Student(1L, "María González", "5to", "A", "Carmen González", "100001"),
        Student(2L, "Juan Perez", "5to", "A", "Luis Perez", "100002"),
        Student(3L, "Ana Rodríguez", "5to", "A", "Elena Rodríguez", null),
        Student(4L, "Carlos Martínez", "5to", "A", "José Martínez", "100004"),
        Student(5L, "Sofía López", "5to", "A", "Carla López", null),
        Student(6L, "Diego Sánchez", "5to", "A", "Mario Sánchez", "100006")
    )

    private var gradeStore: List<Grade> = mockStudents.map { student ->
        Grade(
            studentId = student.id,
            subject = "Matemáticas",
            period = "1er Lapso",
            activityName = "Evaluación 1",
            activityType = "EXAM",
            score = gradesMockStudents.firstOrNull { it.id == student.id }?.score?.toDouble() ?: 0.0,
            maxScore = 20.0,
            synced = false
        )
    }

    init {
        loadData()
    }

    fun onEvent(event: GradesEvent) {
        when (event) {
            GradesEvent.LoadData -> loadData()
            is GradesEvent.SubjectSelected -> {
                _uiState.update { it.copy(selectedSubject = event.subject) }
                refreshDerivedMetrics()
            }

            is GradesEvent.PeriodSelected -> {
                _uiState.update { it.copy(selectedPeriod = event.period) }
                refreshDerivedMetrics()
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
            GradesEvent.SaveGrades -> saveGrades()
            GradesEvent.SendBulletinClicked -> sendBulletin()
            GradesEvent.ClearMessages -> {
                _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            }
        }
    }

    private fun loadData() {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                isLoading = false,
                studentsDomain = mockStudents,
                gradesDomain = gradeStore,
                students = studentsFromDomain(mockStudents),
                courseName = "${mockStudents.first().grade} Grado Sección ${mockStudents.first().section}",
                subjects = listOf("Matemáticas", "Lengua", "Ciencias", "Estudios Sociales"),
                periods = listOf("1er Lapso", "2do Lapso", "3er Lapso"),
                selectedSubject = current.selectedSubject,
                selectedPeriod = current.selectedPeriod
            )
        }
        refreshDerivedMetrics()
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

        val existing = gradeStore.indexOfFirst {
            it.studentId == studentId &&
                it.subject == subject &&
                it.period == period &&
                it.activityName == activityName
        }

        val updated = Grade(
            id = if (existing >= 0) gradeStore[existing].id else 0L,
            studentId = studentId,
            subject = subject,
            period = period,
            activityName = activityName,
            activityType = activityType,
            score = normalizedScore,
            maxScore = maxScore,
            synced = false
        )

        gradeStore = if (existing >= 0) {
            gradeStore.toMutableList().apply { this[existing] = updated }
        } else {
            gradeStore + updated
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
            delay(150)
            _uiState.update { it.copy(isSaving = false, successMessage = "Calificaciones guardadas localmente.") }
        }
    }

    private fun sendBulletin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null, successMessage = null) }
            delay(150)
            _uiState.update { it.copy(isSending = false, successMessage = "Boletín preparado para envío.") }
        }
    }

    private fun studentsFromDomain(students: List<Student>): List<GradeStudentMock> = students.map { student ->
        gradesMockStudents.firstOrNull { it.id == student.id }?.copy(name = student.fullName)
            ?: GradeStudentMock(
                id = student.id,
                name = student.fullName,
                score = null,
                status = GradeVisualStatus.NOT_REGISTERED
            )
    }

    companion object {
        fun mock(): GradesViewModel = GradesViewModel(
            initialState = GradesUiState(students = gradesMockStudents)
        )
    }
}
