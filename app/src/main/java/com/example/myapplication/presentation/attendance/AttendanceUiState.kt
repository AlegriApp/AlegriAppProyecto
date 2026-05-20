package com.example.myapplication.presentation.attendance

import androidx.compose.runtime.saveable.listSaver

data class AttendanceUiState(
    val screenTitle: String = "Toma de Asistencia",
    val dateLabel: String = "Fecha: miércoles, 6 de mayo de 2026",
    val courseName: String = "5to Grado Sección A",
    val students: List<AttendanceStudentUi> = emptyList(),
    val statusByStudent: Map<Long, AttendanceStatus> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val reportPreview: AttendanceReportPreview? = null
) {
    val summary: AttendanceSummary
        get() = AttendanceSummary.from(students, statusByStudent)

    val registeredCount: Int
        get() = summary.registeredCount

    fun withSelectedStatus(studentId: Long, status: AttendanceStatus): AttendanceUiState {
        val updatedStatuses = statusByStudent.toMutableMap().apply {
            this[studentId] = status
        }
        return copy(statusByStudent = updatedStatuses)
    }

    companion object {
        val Saver = listSaver<AttendanceUiState, Any>(
            save = { state ->
                listOf(
                    state.screenTitle,
                    state.dateLabel,
                    state.courseName,
                    state.students.flatMap { student ->
                        listOf(student.id, student.name, student.gradeSection)
                    },
                    state.statusByStudent.flatMap { (studentId, status) ->
                        listOf(studentId, status.name)
                    },
                    state.isLoading,
                    state.errorMessage ?: ""
                )
            },
            restore = { restored ->
                val flattenedStudents = restored[3] as List<*>
                val flattenedStatuses = restored[4] as List<*>
                AttendanceUiState(
                    screenTitle = restored[0] as String,
                    dateLabel = restored[1] as String,
                    courseName = restored[2] as String,
                    students = flattenedStudents.chunked(3).map { studentFields ->
                        AttendanceStudentUi(
                            id = studentFields[0] as Long,
                            name = studentFields[1] as String,
                            gradeSection = studentFields[2] as String
                        )
                    },
                    statusByStudent = flattenedStatuses.chunked(2).associate { statusFields ->
                        val studentId = statusFields[0] as Long
                        val status = AttendanceStatus.valueOf(statusFields[1] as String)
                        studentId to status
                    },
                    isLoading = restored[5] as Boolean,
                    errorMessage = (restored[6] as String).takeIf { it.isNotBlank() }
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

enum class AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    UNMARKED
}

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
                absentCount = statusValues.count { it == AttendanceStatus.ABSENT },
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

fun attendanceMockUiState(): AttendanceUiState = AttendanceUiState(
    students = listOf(
        AttendanceStudentUi(id = 1L, name = "María González", gradeSection = "5to A"),
        AttendanceStudentUi(id = 2L, name = "Juan Pérez", gradeSection = "5to A"),
        AttendanceStudentUi(id = 3L, name = "Ana Rodríguez", gradeSection = "5to A"),
        AttendanceStudentUi(id = 4L, name = "Carlos Martínez", gradeSection = "5to A"),
        AttendanceStudentUi(id = 5L, name = "Sofía López", gradeSection = "5to A"),
        AttendanceStudentUi(id = 6L, name = "Diego Sánchez", gradeSection = "5to A")
    ),
    statusByStudent = emptyMap()
)
