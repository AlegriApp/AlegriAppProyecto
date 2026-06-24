package com.example.myapplication.presentation.attendance



import androidx.lifecycle.ViewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope

import android.net.Uri

import com.example.myapplication.core.network.NetworkMonitor

import com.example.myapplication.domain.model.Attendance

import com.example.myapplication.domain.model.sync.SyncOutcome

import com.example.myapplication.domain.repository.CatalogRepository

import com.example.myapplication.domain.repository.SyncRepository

import com.example.myapplication.domain.usecase.attendance.GetAttendanceByDateAndCourseUseCase

import com.example.myapplication.domain.model.AttendanceStatus

import com.example.myapplication.domain.service.AttendanceTranscriptionService

import com.example.myapplication.domain.service.AttendanceTranscriptionStudent


import com.example.myapplication.domain.usecase.ocr.RecognizeTextFromImageUseCase

import com.example.myapplication.domain.usecase.attendance.SaveAttendanceUseCase

import com.example.myapplication.domain.model.telegram.TelegramSendOutcome

import com.example.myapplication.domain.repository.StudentRepository
import com.example.myapplication.domain.usecase.telegram.SendParentTelegramUseCase

import com.example.myapplication.services.telegram.TelegramMessageBuilder

import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.Job

import java.time.LocalDate

import java.time.format.DateTimeFormatter

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch



class AttendanceViewModel(

    private val getAttendanceByDateAndCourseUseCase: GetAttendanceByDateAndCourseUseCase,

    private val catalogRepository: CatalogRepository,

    private val saveAttendanceUseCase: SaveAttendanceUseCase,

    private val recognizeTextFromImageUseCase: RecognizeTextFromImageUseCase,

    private val attendanceTranscriptionService: AttendanceTranscriptionService = AttendanceTranscriptionService(),

    private val studentRepository: StudentRepository,
    private val sendParentTelegramUseCase: SendParentTelegramUseCase,

    private val networkMonitor: NetworkMonitor? = null,

    private val syncRepository: SyncRepository? = null,

    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),

    initialState: AttendanceUiState = AttendanceUiState(

        isLoading = true,

        selectedDate = currentDate(),

        dateLabel = "Fecha: ${currentDateLabel()}"

    )

) : ViewModel() {

    private val _uiState = MutableStateFlow(
        initialState.copy(
            selectedDate = savedStateHandle[KEY_SELECTED_DATE] ?: initialState.selectedDate,
            dateLabel = savedStateHandle.get<String>(KEY_SELECTED_DATE)
                ?.let { "Fecha: ${toHumanDate(it)}" }
                ?: initialState.dateLabel,
            selectedCourseId = savedStateHandle[KEY_SELECTED_COURSE_ID],
            selectedSubjectId = savedStateHandle[KEY_SELECTED_SUBJECT_ID],
            detectedOcrText = savedStateHandle[KEY_DETECTED_OCR_TEXT] ?: initialState.detectedOcrText
        )
    )

    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    private var subjectsJob: Job? = null

    private var hasPendingLocalEdits = false



    init {

        viewModelScope.launch {
            uiState.collect(::persistRestorableState)
        }

        networkMonitor?.let { monitor ->

            viewModelScope.launch {

                monitor.isOnline.collect { online ->

                    _uiState.update { it.copy(isOffline = !online) }

                }

            }

        }

        loadCoursesAndSync()

    }



    private fun loadCoursesAndSync() {

        viewModelScope.launch(Dispatchers.IO) {

            val syncMessage = runCatching {

                val catalog = catalogRepository.syncCatalogsFromRemote()

                val all = syncRepository?.syncAll()

                listOfNotNull(

                    (catalog as? SyncOutcome.Success)?.message,

                    (all as? SyncOutcome.Success)?.message

                ).joinToString(" ")

            }.getOrElse { it.message.orEmpty() }

            if (syncMessage.isNotBlank()) {

                _uiState.update { it.copy(successMessage = syncMessage) }

            }

        }

        viewModelScope.launch {

            catalogRepository.observeCourses().collect { courses ->

                val options = courses.map { it.id to it.displayName }

                val selected = _uiState.value.selectedCourseId ?: options.firstOrNull()?.first

                _uiState.update {

                    it.copy(

                        courses = options,

                        selectedCourseId = selected,

                        courseName = options.firstOrNull { pair -> pair.first == selected }?.second

                            ?: it.courseName

                    )

                }

                selected?.let { loadSubjectsForCourse(it) }

            }

        }

    }



    fun onEvent(event: AttendanceEvent) {

        when (event) {

            AttendanceEvent.LoadStudents -> restartObserveIfReady()

            is AttendanceEvent.CourseSelected -> onCourseSelected(event.courseId)

            is AttendanceEvent.SubjectSelected -> onSubjectSelected(event.subjectId)

            is AttendanceEvent.ChangeDate -> changeDate(event.selectedDate)

            is AttendanceEvent.MarkPresent -> updateStatus(event.studentId, AttendanceStatus.PRESENT)

            is AttendanceEvent.MarkAbsent -> updateStatus(event.studentId, AttendanceStatus.ABSENT)

            AttendanceEvent.MarkAllPresent -> markAllPresent()

            AttendanceEvent.ClearMarks -> clearMarks()

            AttendanceEvent.SaveAttendance -> saveAttendance()

            AttendanceEvent.SendReport -> sendReport()

            is AttendanceEvent.OcrImageSelected -> processOcr(event.uri)

            is AttendanceEvent.TranscriptionTextChanged -> updateTranscriptionText(event.text)

            AttendanceEvent.ApplyOcrSuggestions -> applyOcrSuggestions()

            AttendanceEvent.ClearMessages -> clearMessages()

        }

    }



    private fun onCourseSelected(courseId: Long) {

        val label = _uiState.value.courses.firstOrNull { it.first == courseId }?.second ?: "Curso"

        _uiState.update {

            it.copy(

                selectedCourseId = courseId,

                courseName = label,

                selectedSubjectId = null,

                subjects = emptyList(),

                subjectName = "",

                students = emptyList(),

                attendanceByStudent = emptyMap()

            ).recalculateSummary()

        }

        loadSubjectsForCourse(courseId)

        viewModelScope.launch(Dispatchers.IO) {

            syncRepository?.syncStudentsFromRemote()?.let { outcome ->

                if (outcome is SyncOutcome.Success) {

                    _uiState.update { it.copy(successMessage = outcome.message) }

                }

            }

        }

    }



    private fun onSubjectSelected(subjectId: Long) {

        val label = _uiState.value.subjects.firstOrNull { it.first == subjectId }?.second ?: "Materia"

        _uiState.update { it.copy(selectedSubjectId = subjectId, subjectName = label) }

        restartObserveIfReady()

    }



    private fun loadSubjectsForCourse(courseId: Long) {

        subjectsJob?.cancel()

        subjectsJob = viewModelScope.launch {

            catalogRepository.observeSubjectsByCourse(courseId).collect { subjects ->

                val options = subjects.map { it.id to it.nombre }

                val selected = _uiState.value.selectedSubjectId ?: options.firstOrNull()?.first

                _uiState.update {

                    it.copy(

                        subjects = options,

                        selectedSubjectId = selected,

                        subjectName = options.firstOrNull { pair -> pair.first == selected }?.second.orEmpty()

                    )

                }

                restartObserveIfReady()

            }

        }

    }



    private fun restartObserveIfReady() {

        val state = _uiState.value

        val courseId = state.selectedCourseId ?: return

        val subjectId = state.selectedSubjectId ?: return

        observeAttendanceForDate(state.selectedDate, courseId, subjectId)

    }



    private fun observeAttendanceForDate(date: String, courseId: Long, subjectId: Long) {

        observeJob?.cancel()

        hasPendingLocalEdits = false

        observeJob = viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            getAttendanceByDateAndCourseUseCase(date, courseId, subjectId).collect { records ->

                val students = records.map { record ->

                    AttendanceStudentUi(

                        id = record.student.id,

                        name = record.student.fullName,

                        gradeSection = "${record.student.grade} ${record.student.section}",

                        aliases = record.student.aliases

                    )

                }

                val statusFromDb = records

                    .mapNotNull { record ->

                        record.attendance?.let { attendance ->

                            attendance.studentId to attendance.status

                        }

                    }

                    .toMap()

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

                        isLoading = false

                    ).recalculateSummary()

                }

            }

        }

    }



    private fun changeDate(selectedDate: String) {

        _uiState.update { it.copy(selectedDate = selectedDate) }

        restartObserveIfReady()

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

            val courseId = current.selectedCourseId

            val subjectId = current.selectedSubjectId

            if (courseId == null) {

                _uiState.update { it.copy(isSaving = false, errorMessage = "Selecciona un curso.") }

                return@launch

            }

            if (subjectId == null) {

                _uiState.update { it.copy(isSaving = false, errorMessage = "Selecciona una materia.") }

                return@launch

            }

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

                            courseId = courseId,

                            subjectId = subjectId,

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

                subjectName = currentState.subjectName,

                totalStudents = currentState.students.size,

                summary = currentState.summary,

                entries = entries

            )

            var sent = 0
            var failed = 0
            val errors = mutableListOf<String>()
            currentState.students.forEach { row ->
                val status = currentState.attendanceByStudent[row.id] ?: AttendanceStatus.UNMARKED
                val student = studentRepository.findById(row.id)
                    ?: com.example.myapplication.domain.model.Student(
                        id = row.id,
                        fullName = row.name,
                        grade = row.gradeSection,
                        section = "",
                        representativeName = ""
                    )
                val message = TelegramMessageBuilder.buildAttendanceReportForParent(
                    student = student,
                    status = status,
                    date = currentState.selectedDate,
                    courseName = currentState.courseName,
                    subjectName = currentState.subjectName
                )
                when (val outcome = sendParentTelegramUseCase(student, message)) {
                    is TelegramSendOutcome.Success -> sent++
                    is TelegramSendOutcome.Failure -> {
                        failed++
                        errors += "${row.name}: ${outcome.message}"
                    }
                }
            }
            val summary = when {
                sent > 0 && failed == 0 ->
                    "Reporte enviado a $sent representante(s) por Telegram."
                sent > 0 ->
                    "Enviado a $sent representante(s). Fallos: $failed. ${errors.firstOrNull().orEmpty()}"
                else ->
                    errors.firstOrNull() ?: "No se pudo enviar a ningún representante."
            }
            _uiState.update {
                currentState.copy(
                    isSending = false,
                    reportPreview = reportPreview,
                    successMessage = if (sent > 0) summary else null,
                    errorMessage = if (sent == 0) summary else if (failed > 0) summary else null
                )
            }

        }

    }



    private fun clearMessages() {

        _uiState.update { it.copy(errorMessage = null, successMessage = null) }

    }



    private fun updateTranscriptionText(text: String) {

        _uiState.update {

            it.copy(

                detectedOcrText = text,

                errorMessage = null,

                successMessage = null

            )

        }

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

        val state = _uiState.value

        if (state.detectedOcrText.isBlank()) {

            _uiState.update { it.copy(errorMessage = "No hay texto OCR para procesar.") }

            return

        }

        val matches = attendanceTranscriptionService.process(

            transcribedText = state.detectedOcrText,

            students = state.students.map { student ->

                AttendanceTranscriptionStudent(

                    id = student.id,

                    fullName = student.name,

                    aliases = student.aliases

                )

            }

        )

        if (matches.isEmpty()) {

            _uiState.update {

                it.copy(errorMessage = "No se encontraron coincidencias de asistencia en el texto.")

            }

            return

        }

        hasPendingLocalEdits = true

        val detectedStatuses = matches.associate { match -> match.studentId to match.status }

        _uiState.update {

            val updatedStatuses = it.attendanceByStudent + detectedStatuses

            it.copy(

                attendanceByStudent = updatedStatuses,

                errorMessage = null,

                successMessage = "Se aplicaron ${matches.size} coincidencias. Verifica antes de guardar."

            ).recalculateSummary()

        }

    }



    companion object {
        private const val KEY_SELECTED_DATE = "attendance.selected_date"
        private const val KEY_SELECTED_COURSE_ID = "attendance.selected_course_id"
        private const val KEY_SELECTED_SUBJECT_ID = "attendance.selected_subject_id"
        private const val KEY_DETECTED_OCR_TEXT = "attendance.detected_ocr_text"

        private fun currentDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)



        private fun currentDateLabel(): String = toHumanDate(currentDate())



        private fun toHumanDate(date: String): String {

            val localDate = runCatching { LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) }

                .getOrElse { return date }

            return localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        }

        private fun persistableText(value: String): String = value

    }

    private fun persistRestorableState(state: AttendanceUiState) {
        savedStateHandle[KEY_SELECTED_DATE] = state.selectedDate
        savedStateHandle[KEY_SELECTED_COURSE_ID] = state.selectedCourseId
        savedStateHandle[KEY_SELECTED_SUBJECT_ID] = state.selectedSubjectId
        savedStateHandle[KEY_DETECTED_OCR_TEXT] = persistableText(state.detectedOcrText)

    }

}


