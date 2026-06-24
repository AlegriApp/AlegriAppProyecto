package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun observeIncidents(): Flow<List<Incident>>
    suspend fun saveIncident(incident: Incident): Long
    suspend fun getIncidentById(incidentId: Long): Incident?
    suspend fun markIncidentAsSent(incidentId: Long)

    /**
     * Incidentes locales guardados que aún no se enviaron por Telegram.
     * Excluye los provenientes de PULL (que viven en Supabase) y los eliminados.
     */
    suspend fun getPendingTelegramSend(): List<Incident>

    /** Conteo reactivo para mostrar en UI cuántos hay pendientes de envío. */
    fun observePendingTelegramCount(): Flow<Int>
}
