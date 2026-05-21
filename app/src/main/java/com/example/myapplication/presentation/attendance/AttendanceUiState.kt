package com.example.myapplication.presentation.attendance

import androidx.compose.runtime.saveable.listSaver
import com.example.myapplication.domain.model.AttendanceStatus as DomainAttendanceStatus
import com.example.myapplication.domain.model.Student

data class AttendanceUiState(
    val screenTitle: String = "Toma de Asistencia",
    val dateLabel: String = "",
    val selectedDate: String = "",
    val courseName: String = "",
    val students: List<AttendanceStudentUi> = emptyList(),
    val attendanceByStudent: Map<Long, AttendanceStatus> = emptyMap(),
    val presentCount: Int = 0,
    val lateCount: Int = 0,
    val absentCount: Int = 0,
    val unmarkedCount: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSending: Boolean = false,
    val isProcessingOcr: Boolean = false,
    val detectedOcrText: String = "",
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val reportPreview: AttendanceReportPreview? = null
) {
    val statusByStudent: Map<Long, AttendanceStatus>
        get() = attendanceByStudent

    val summary: AttendanceSummary
        get() = AttendanceSummary(
            presentCount = presentCount,
            lateCount = lateCount,
            absentCount = absentCount,
            unmarkedCount = unmarkedCount
        )

    val registeredCount: Int
        get() = summary.registeredCount

    fun withSelectedStatus(studentId: Long, status: AttendanceStatus): AttendanceUiState {
        val updatedStatuses = attendanceByStudent.toMutableMap().apply {
            this[studentId] = status
        }
        return copy(attendanceByStudent = updatedStatuses).recalculateSummary()
    }

    fun recalculateSummary(): AttendanceUiState {
        val statusValues = students.map { student ->
            attendanceByStudent[student.id] ?: AttendanceStatus.UNMARKED
        }
        return copy(
            presentCount = statusValues.count { it == AttendanceStatus.PRESENT },
            lateCount = statusValues.count { it == AttendanceStatus.LATE },
            absentCount = statusValues.count {
                it == AttendanceStatus.ABSENT || it == AttendanceStatus.JUSTIFIED
            },
            unmarkedCount = statusValues.count { it == AttendanceStatus.UNMARKED }
        )
    }

    companion object {
        val Saver = listSaver<AttendanceUiState, Any>(
            save = { state ->
                listOf(
                    state.screenTitle,
                    state.dateLabel,
                    state.selectedDate,
                    state.courseName,
                    state.students.flatMap { student ->
                        listOf(student.id, student.name, student.gradeSection)
                    },
                    state.attendanceByStudent.flatMap { (studentId, status) ->
                        listOf(studentId, status.name)
                    },
                    state.isLoading,
                    state.isSaving,
                    state.isSending,
                    state.isProcessingOcr,
                    state.detectedOcrText,
                    state.presentCount,
                    state.lateCount,
                    state.absentCount,
                    state.unmarkedCount,
                    state.successMessage ?: "",
                    state.errorMessage ?: ""
                )
            },
            restore = { restored ->
                val flattenedStudents = restored[4] as List<*>
                val flattenedStatuses = restored[5] as List<*>
                AttendanceUiState(
                    screenTitle = restored[0] as String,
                    dateLabel = restored[1] as String,
                    selectedDate = restored[2] as String,
                    courseName = restored[3] as String,
                    students = flattenedStudents.chunked(3).map { studentFields ->
                        AttendanceStudentUi(
                            id = studentFields[0] as Long,
                            name = studentFields[1] as String,
                            gradeSection = studentFields[2] as String
                        )
                    },
                    attendanceByStudent = flattenedStatuses.chunked(2).associate { statusFields ->
                        val studentId = statusFields[0] as Long
                        val status = AttendanceStatus.valueOf(statusFields[1] as String)
                        studentId to status
                    },
                    isLoading = restored[6] as Boolean,
                    isSaving = restored[7] as Boolean,
                    isSending = restored[8] as Boolean,
                    isProcessingOcr = restored[9] as Boolean,
                    detectedOcrText = restored[10] as String,
                    presentCount = restored[11] as Int,
                    lateCount = restored[12] as Int,
                    absentCount = restored[13] as Int,
                    unmarkedCount = restored[14] as Int,
                    successMessage = (restored[15] as String).takeIf { it.isNotBlank() },
                    errorMessage = (restored[16] as String).takeIf { it.isNotBlank() }
                )
            }
        )
    }
}

data class AttendanceStudentUi(
    val id: Long,
    val name: String,
    val gradeSection: String
)

data class AttendanceSummary(
    val presentCount: Int,
    val lateCount: Int,
    val absentCount: Int,
    val unmarkedCount: Int
) {
    val registeredCount: Int
        get() = presentCount + lateCount + absentCount

    companion object {
        fun from(
            students: List<AttendanceStudentUi>,
            statusByStudent: Map<Long, AttendanceStatus>
        ): AttendanceSummary {
            val statusValues = students.map { student ->
                statusByStudent[student.id] ?: AttendanceStatus.UNMARKED
            }
            return AttendanceSummary(
                presentCount = statusValues.count { it == AttendanceStatus.PRESENT },
                lateCount = statusValues.count { it == AttendanceStatus.LATE },
                absentCount = statusValues.count {
                    it == AttendanceStatus.ABSENT || it == AttendanceStatus.JUSTIFIED
                },
                unmarkedCount = statusValues.count { it == AttendanceStatus.UNMARKED }
            )
        }
    }
}

data class AttendanceReportPreview(
    val dateLabel: String,
    val courseName: String,
    val totalStudents: Int,
    val summary: AttendanceSummary,
    val entries: List<AttendanceReportEntry>
)

data class AttendanceReportEntry(
    val studentId: Long,
    val studentName: String,
    val status: AttendanceStatus
)

data class AttendanceStudent(
    val student: Student,
    val status: AttendanceStatus = AttendanceStatus.UNMARKED
)

fun attendanceMockUiState(): AttendanceUiState = AttendanceUiState(
    students = listOf(
        AttendanceStudentUi(id = 1L, name = "María González", gradeSection = "5to A"),
        AttendanceStudentUi(id = 2L, name = "Juan Pérez", gradeSection = "5to A"),
        AttendanceStudentUi(id = 3L, name = "Ana Rodríguez", gradeSection = "5to A"),
        AttendanceStudentUi(id = 4L, name = "Carlos Martínez", gradeSection = "5to A"),
        AttendanceStudentUi(id = 5L, name = "Sofía López", gradeSection = "5to A"),
        AttendanceStudentUi(id = 6L, name = "Diego Sánchez", gradeSection = "5to A")
    ),
    attendanceByStudent = emptyMap()
).recalculateSummary()

typealias AttendanceStatus = DomainAttendanceStatus
