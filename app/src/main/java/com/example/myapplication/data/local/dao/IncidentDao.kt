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

    /**
     * Incidentes que el usuario guardó pero aún no fueron enviados por Telegram.
     * Se usa al recuperar conexión para auto-enviar el backlog.
     *
     * **No** se filtra por `local_only` porque el push a Supabase (Fase 14) ya
     * pone esa columna en `0` en cuanto el incidente llega al servidor, lo que
     * sacaría al registro de esta cola antes de que Telegram lo procese.
     *
     * El filtro real para excluir los provenientes de PULL es `enviado = 0`:
     * los incidentes PULLed se hidratan con `sent = true` en el mapper, por
     * lo que ya quedan excluidos por esta condición.
     */
    @Query(
        "SELECT * FROM incidentes " +
            "WHERE enviado = 0 AND is_deleted = 0 " +
            "ORDER BY fecha_hora ASC"
    )
    suspend fun getPendingTelegramSend(): List<IncidentEntity>

    @Query(
        "SELECT COUNT(*) FROM incidentes " +
            "WHERE enviado = 0 AND is_deleted = 0"
    )
    fun observePendingTelegramCount(): kotlinx.coroutines.flow.Flow<Int>

    // ---------- Soft-delete ----------

    @Query("UPDATE incidentes SET is_deleted = 1 WHERE uuid = :uuid")
    suspend fun softDelete(uuid: String)

    // ---------- PUSH a Supabase (Fase 14) ----------
    //
    // En Fase 0 se decidió PULL only. En Fase 14 el equipo revirtió esa decisión:
    // mobile ahora SÍ envía incidentes locales a Supabase. La cola de push
    // filtra por sync_status pendiente y excluye los que vinieron del PULL
    // (sync_status='SUCCESS' al hidratarse desde el servidor).

    @Query(
        "SELECT * FROM incidentes " +
            "WHERE sync_status IN ('IDLE','ERROR') AND is_deleted = 0"
    )
    suspend fun getPendingPushToSupabase(): List<IncidentEntity>

    @Query(
        "UPDATE incidentes SET sync_status = 'SENDING', last_sync_attempt = :now " +
            "WHERE uuid = :uuid"
    )
    suspend fun markPushSending(uuid: String, now: Long)

    @Query(
        "UPDATE incidentes SET sync_status = 'SUCCESS', sync_error = NULL, " +
            "remote_id = :remoteId, server_updated_at = :serverTs, " +
            "local_only = 0 " +
            "WHERE uuid = :uuid"
    )
    suspend fun markPushSynced(uuid: String, remoteId: Long?, serverTs: Long?)

    @Query(
        "UPDATE incidentes SET sync_status = 'ERROR', sync_error = :error, " +
            "last_sync_attempt = :now WHERE uuid = :uuid"
    )
    suspend fun markPushFailed(uuid: String, error: String, now: Long)

    @Query("SELECT COUNT(*) FROM incidentes WHERE local_only = 1 AND is_deleted = 0")
    fun observeLocalOnlyCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM incidentes WHERE is_deleted = 0")
    fun observeTotalCount(): Flow<Int>
}
