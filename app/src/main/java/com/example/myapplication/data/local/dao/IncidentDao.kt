package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Query("SELECT * FROM incidentes ORDER BY fecha_hora DESC")
    fun observeIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidentes WHERE id = :incidentId LIMIT 1")
    suspend fun getIncidentById(incidentId: Long): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceIncident(incident: IncidentEntity): Long

    @Query("UPDATE incidentes SET enviado = 1 WHERE id = :incidentId")
    suspend fun markAsSent(incidentId: Long)
}
