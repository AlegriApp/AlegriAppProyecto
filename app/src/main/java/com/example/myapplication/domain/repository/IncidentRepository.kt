package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.Incident
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun observeIncidents(): Flow<List<Incident>>
    suspend fun saveIncident(incident: Incident): Long
    suspend fun getIncidentById(incidentId: Long): Incident?
    suspend fun markIncidentAsSent(incidentId: Long)
}
