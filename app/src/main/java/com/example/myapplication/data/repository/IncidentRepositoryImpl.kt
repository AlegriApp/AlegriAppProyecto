package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.IncidentDao
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.mapper.toEntity
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncidentRepositoryImpl(
    private val incidentDao: IncidentDao
) : IncidentRepository {
    override fun observeIncidents(): Flow<List<Incident>> =
        incidentDao.observeIncidents().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveIncident(incident: Incident): Long =
        incidentDao.insertOrReplaceIncident(incident.toEntity(existingId = incident.id.takeIf { it != 0L }))

    override suspend fun getIncidentById(incidentId: Long): Incident? =
        incidentDao.getIncidentById(incidentId)?.toDomain()

    override suspend fun markIncidentAsSent(incidentId: Long) {
        incidentDao.markAsSent(incidentId)
    }
}
