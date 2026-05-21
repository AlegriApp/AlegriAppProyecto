package com.example.myapplication.presentation.incidents

import androidx.compose.runtime.saveable.listSaver

data class IncidentUiState(
    val appTitle: String = "AlegriApp",
    val appSubtitle: String = "Sistema de Comunicaci\u00f3n Docente - Fe y Alegr\u00eda",
    val screenTitle: String = "Reporte de Incidentes",
    val screenDescription: String = "Env\u00edo directo a autoridades y representantes v\u00eda Telegram",
    val students: List<IncidentStudentUi> = incidentMockStudents,
    val selectedStudentId: Long? = null,
    val selectedType: IncidentTypeOption = IncidentTypeOption.BEHAVIOR,
    val description: String = ""
) {
    companion object {
        val Saver = listSaver<IncidentUiState, Any?>(
            save = { state ->
                listOf(
                    state.appTitle,
                    state.appSubtitle,
                    state.screenTitle,
                    state.screenDescription,
                    state.students.flatMap { student -> listOf(student.id, student.name) },
                    state.selectedStudentId,
                    state.selectedType.name,
                    state.description
                )
            },
            restore = { restored ->
                val flattenedStudents = restored[4] as List<*>
                IncidentUiState(
                    appTitle = restored[0] as String,
                    appSubtitle = restored[1] as String,
                    screenTitle = restored[2] as String,
                    screenDescription = restored[3] as String,
                    students = flattenedStudents.chunked(2).map { studentFields ->
                        IncidentStudentUi(
                            id = studentFields[0] as Long,
                            name = studentFields[1] as String
                        )
                    },
                    selectedStudentId = restored[5] as Long?,
                    selectedType = IncidentTypeOption.valueOf(restored[6] as String),
                    description = restored[7] as String
                )
            }
        )
    }
}

data class IncidentStudentUi(
    val id: Long,
    val name: String
)

enum class IncidentTypeOption(val label: String) {
    BEHAVIOR("Comportamiento"),
    ACADEMIC("Acad\u00e9mico"),
    HEALTH("Salud"),
    OTHER("Otro")
}

val incidentMockStudents = listOf(
    IncidentStudentUi(1L, "Mar\u00eda Gonz\u00e1lez"),
    IncidentStudentUi(2L, "Juan P\u00e9rez"),
    IncidentStudentUi(3L, "Ana Rodr\u00edguez"),
    IncidentStudentUi(4L, "Carlos Mart\u00ednez")
)
