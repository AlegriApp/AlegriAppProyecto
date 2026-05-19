package com.example.myapplication.presentation.attendance

import androidx.compose.runtime.saveable.listSaver

data class AttendanceUiState(
    val screenTitle: String = "Toma de Asistencia",
    val dateLabel: String = "Fecha: miércoles, 6 de mayo de 2026",
    val courseName: String = "5to Grado Sección A",
    val students: List<AttendanceStudentUi> = emptyList()
) {
    companion object {
        val Saver = listSaver<AttendanceUiState, Any>(
            save = { state ->
                listOf(
                    state.screenTitle,
                    state.dateLabel,
                    state.courseName,
                    state.students.flatMap { student ->
                        listOf(student.id, student.name, student.gradeSection)
                    }
                )
            },
            restore = { restored ->
                val flattenedStudents = restored[3] as List<*>
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
                    }
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

fun attendanceMockUiState(): AttendanceUiState = AttendanceUiState(
    students = listOf(
        AttendanceStudentUi(id = 1L, name = "María González", gradeSection = "5to A"),
        AttendanceStudentUi(id = 2L, name = "Juan Pérez", gradeSection = "5to A"),
        AttendanceStudentUi(id = 3L, name = "Ana Rodríguez", gradeSection = "5to A"),
        AttendanceStudentUi(id = 4L, name = "Carlos Martínez", gradeSection = "5to A"),
        AttendanceStudentUi(id = 5L, name = "Sofía López", gradeSection = "5to A"),
        AttendanceStudentUi(id = 6L, name = "Diego Sánchez", gradeSection = "5to A")
    )
)
