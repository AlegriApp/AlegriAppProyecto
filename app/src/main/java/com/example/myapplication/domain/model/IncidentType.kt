package com.example.myapplication.domain.model

enum class IncidentType(val label: String) {
    BEHAVIOR("Comportamiento / Conducta"),
    ACADEMIC("Academico"),
    HEALTH("Salud"),
    OTHER("Otro")
}

enum class IncidentSeverity(val label: String) {
    LOW("Baja"),
    MEDIUM("Media"),
    HIGH("Alta")
}
