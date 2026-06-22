package com.example.myapplication.presentation.grades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.common.GradeScale
import com.example.myapplication.core.network.NetworkMonitor
import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.repository.SyncRepository
import com.example.myapplication.domain.usecase.grade.GetGradesByStudentUseCase
import com.example.myapplication.domain.usecase.student.GetStudentByIdUseCase
import com.example.myapplication.presentation.grades.components.GradeDetailEntry
import com.example.myapplication.presentation.grades.components.GradeDetailStudent
import com.example.myapplication.presentation.grades.components.GradeDetailSubject
import com.example.myapplication.presentation.grades.components.GradeVisualStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de **detalle de calificaciones** de un estudiante.
 *
 * Reemplaza los datos *mock* anteriores (`findMockDetailById` + `delay`) por el
 * flujo real offline-first del proyecto:
 *   - `getStudentByIdUseCase`  → observa el estudiante en Room.
 *   - `getGradesByStudentUseCase` → observa sus calificaciones en Room.
 *   - `networkMonitor`         → marca si los datos mostrados son cache offline.
 *   - `syncRepository`         → permite forzar una sincronización ("Sincronizar ahora").
 *
 * Combina ambos flujos y los proyecta a [GradeDetailUiState] (sealed class),
 * cubriendo Loading / Empty / Success / Error de forma exhaustiva.
 */
class GradeDetailViewModel(
    private val studentId: Long,
    getStudentByIdUseCase: GetStudentByIdUseCase,
    getGradesByStudentUseCase: GetGradesByStudentUseCase,
    private val networkMonitor: NetworkMonitor? = null,
    private val syncRepository: SyncRepository? = null
) : ViewModel() {

    private val onlineFlow = networkMonitor?.isOnline ?: flowOf(true)

    private val _uiState = MutableStateFlow<GradeDetailUiState>(GradeDetailUiState.Loading)
    val uiState: StateFlow<GradeDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getStudentByIdUseCase(studentId),
                getGradesByStudentUseCase(studentId),
                onlineFlow
            ) { student, grades, isOnline ->
                buildState(student, grades, isOnline)
            }.collect { state -> _uiState.value = state }
        }
        // Al abrir el detalle intentamos una sincronización (no bloqueante).
        refresh()
    }

    /** Fuerza una pasada de sincronización con Supabase (botón "Sincronizar ahora"). */
    fun refresh() {
        val repo = syncRepository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.syncAll() }
        }
    }

    private fun buildState(
        student: Student?,
        grades: List<Grade>,
        isOnline: Boolean
    ): GradeDetailUiState {
        if (student == null) {
            return GradeDetailUiState.Error("No se encontró el estudiante solicitado.")
        }
        if (grades.isEmpty()) {
            return GradeDetailUiState.Empty
        }
        return GradeDetailUiState.Success(
            student = buildDetailStudent(student, grades),
            grades = grades,
            // Mostramos el banner de "datos locales" cuando no hay conexión.
            isFromCache = !isOnline
        )
    }

    /** Proyecta el modelo de dominio a la vista [GradeDetailStudent] que usan los Composables. */
    private fun buildDetailStudent(student: Student, grades: List<Grade>): GradeDetailStudent {
        val subjects = grades
            .groupBy { it.subject }
            .map { (subjectName, subjectGrades) -> buildSubject(subjectName, subjectGrades) }
            .sortedBy { it.name }

        // Promedio general sobre la escala oficial (sobre 10), normalizando cada nota.
        val generalAverage = grades
            .map { GradeScale.normalize(it.score, it.maxScore) }
            .average().takeIf { !it.isNaN() } ?: 0.0
        val approved = subjects.count { it.status == GradeVisualStatus.APPROVED }
        val atRisk = subjects.count { it.status == GradeVisualStatus.AT_RISK }

        return GradeDetailStudent(
            id = student.id,
            fullName = student.fullName,
            course = buildCourseLabel(student),
            period = grades.firstOrNull()?.period?.takeIf { it.isNotBlank() } ?: "—",
            generalAverage = generalAverage,
            status = statusFor(generalAverage),
            approvedSubjects = approved,
            atRiskSubjects = atRisk,
            lastSyncDate = "—",
            subjects = subjects
        )
    }

    private fun buildSubject(subjectName: String, subjectGrades: List<Grade>): GradeDetailSubject {
        // Promedio de la materia sobre la escala oficial (sobre 10).
        val average = subjectGrades
            .map { GradeScale.normalize(it.score, it.maxScore) }
            .average().takeIf { !it.isNaN() } ?: 0.0
        return GradeDetailSubject(
            id = subjectGrades.firstOrNull()?.subjectId
                ?: subjectName.hashCode().toLong(),
            name = subjectName,
            average = average,
            maxScore = GradeScale.MAX_SCORE_INT,
            status = statusFor(average),
            entries = subjectGrades.map { grade ->
                GradeDetailEntry(
                    id = grade.id,
                    activity = grade.activityName,
                    // Cada nota se muestra normalizada sobre 10.
                    score = GradeScale.normalize(grade.score, grade.maxScore),
                    maxScore = GradeScale.MAX_SCORE_INT,
                    // El esquema actual no guarda fecha por nota; mostramos el tipo
                    // de evaluación como contexto de la actividad.
                    date = grade.activityType,
                    teacherComment = grade.observation
                )
            }
        )
    }

    private fun buildCourseLabel(student: Student): String {
        val grade = student.grade.trim()
        val section = student.section.trim()
        return when {
            grade.isNotBlank() && section.isNotBlank() -> "Grado $grade - Sección $section"
            grade.isNotBlank() -> "Grado $grade"
            else -> "Curso no asignado"
        }
    }

    /** Aprobado/En riesgo según la nota mínima oficial ([GradeScale.PASSING_GRADE], sobre 10). */
    private fun statusFor(average: Double): GradeVisualStatus =
        if (average < GradeScale.PASSING_GRADE) GradeVisualStatus.AT_RISK else GradeVisualStatus.APPROVED
}
