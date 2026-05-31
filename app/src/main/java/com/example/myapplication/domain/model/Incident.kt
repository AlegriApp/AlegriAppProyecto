package com.example.myapplication.domain.model

data class Incident(
    val id: Long = 0L,
    val studentId: Long,
    /** Id remoto de `tipos_incidente` como string, o nombre legacy del enum. */
    val type: String,
    val severity: IncidentSeverity = IncidentSeverity.MEDIUM,
    val description: String,
    val dateTime: String,
    val teacherName: String? = null,
    val sent: Boolean = false,
    val syncPending: Boolean = true
)
