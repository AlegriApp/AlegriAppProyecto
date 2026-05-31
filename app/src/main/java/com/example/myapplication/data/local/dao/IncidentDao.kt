package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Query("SELECT * FROM incidentes WHERE is_deleted = 0 ORDER BY fecha_hora DESC")
    fun observeIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidentes WHERE id = :incidentId LIMIT 1")
    suspend fun getIncidentById(incidentId: Long): IncidentEntity?

    @Query("SELECT * FROM incidentes WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceIncident(incident: IncidentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceIncidents(incidents: List<IncidentEntity>)

    @Query("UPDATE incidentes SET enviado = 1 WHERE id = :incidentId")
    suspend fun markAsSent(incidentId: Long)

    // ---------- Soft-delete ----------

    @Query("UPDATE incidentes SET is_deleted = 1 WHERE uuid = :uuid")
    suspend fun softDelete(uuid: String)

    // ---------- PULL only ----------
    // Incidentes nunca se envían a Supabase desde mobile. La cola de sync
    // de incidentes (`getPendingPull`) sirve para distinguir registros que
    // llegaron del servidor y aún no se procesaron localmente.

    @Query("SELECT COUNT(*) FROM incidentes WHERE local_only = 1 AND is_deleted = 0")
    fun observeLocalOnlyCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM incidentes WHERE is_deleted = 0")
    fun observeTotalCount(): Flow<Int>
}
