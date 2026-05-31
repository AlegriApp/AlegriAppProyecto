package com.example.myapplication.data.mapper

import com.example.myapplication.core.common.newUuid
import com.example.myapplication.data.local.entity.IncidentEntity
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.IncidentSeverity
import com.example.myapplication.domain.model.IncidentType
import com.example.myapplication.domain.model.sync.SyncState

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

fun Incident.toEntity(
    existing: IncidentEntity? = null
): IncidentEntity = IncidentEntity(
    id = existing?.id ?: id,
    studentId = studentId,
    type = type.name,
    severity = severity.name,
    description = description,
    dateTime = dateTime,
    teacherName = teacherName,
    sent = sent,
    syncPending = syncPending,
    uuid = existing?.uuid ?: newUuid(),
    remoteId = existing?.remoteId,
    // Incidentes locales NUNCA viajan a Supabase (decisión equipo).
    // Quedan como IDLE (a sync) solo si vinieran del servidor, pero al crear
    // localmente arrancan como IDLE y `localOnly = true` los excluye del push.
    syncStatus = existing?.syncStatus ?: SyncState.Stored.IDLE,
    syncError = existing?.syncError,
    lastSyncAttempt = existing?.lastSyncAttempt,
    serverUpdatedAt = existing?.serverUpdatedAt,
    isDeleted = existing?.isDeleted ?: false,
    localOnly = existing?.localOnly ?: true
)

