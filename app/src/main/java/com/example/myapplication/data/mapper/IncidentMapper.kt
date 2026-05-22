package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.IncidentEntity
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.IncidentSeverity
import com.example.myapplication.domain.model.IncidentType

fun IncidentEntity.toDomain(): Incident = Incident(
    id = id,
    studentId = studentId,
    type = runCatching { IncidentType.valueOf(type) }.getOrDefault(IncidentType.OTHER),
    severity = runCatching { IncidentSeverity.valueOf(severity) }.getOrDefault(IncidentSeverity.MEDIUM),
    description = description,
    dateTime = dateTime,
    teacherName = teacherName,
    sent = sent,
    syncPending = syncPending
)

fun Incident.toEntity(existingId: Long? = null): IncidentEntity = IncidentEntity(
    id = existingId ?: id,
    studentId = studentId,
    type = type.name,
    severity = severity.name,
    description = description,
    dateTime = dateTime,
    teacherName = teacherName,
    sent = sent,
    syncPending = syncPending
)
