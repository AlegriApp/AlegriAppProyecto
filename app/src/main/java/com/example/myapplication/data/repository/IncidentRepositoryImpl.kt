package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.IncidentDao
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.mapper.toEntity
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Incidentes locales son **PULL only** respecto a Supabase.
 *
 * Mobile guarda incidentes en Room para uso local (UI + envío Telegram), pero
 * NUNCA los sincroniza hacia Supabase. La tabla `incidentes` remota se popula
 * por un proceso externo a la app móvil (ver `CONFIG_PENDIENTE_EQUIPO.md` §7).
 *
 * Cuando llega un incidente desde el servidor (vía SyncRepository), se inserta
 * con `localOnly = false` para que la UI lo muestre; los locales mantienen
 * `localOnly = true` y nunca viajan.
 */
class IncidentRepositoryImpl(
    private val incidentDao: IncidentDao
) : IncidentRepository {

    override fun observeIncidents(): Flow<List<Incident>> =
        incidentDao.observeIncidents().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveIncident(incident: Incident): Long {
        val existing = incident.id.takeIf { it != 0L }?.let { incidentDao.getIncidentById(it) }
        return incidentDao.insertOrReplaceIncident(
            incident.toEntity(existing = existing)
        )
    }

    override suspend fun getIncidentById(incidentId: Long): Incident? =
        incidentDao.getIncidentById(incidentId)?.toDomain()

    override suspend fun markIncidentAsSent(incidentId: Long) {
        incidentDao.markAsSent(incidentId)
    }
}
