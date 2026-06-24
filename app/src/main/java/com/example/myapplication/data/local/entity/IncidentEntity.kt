package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myapplication.domain.model.sync.SyncState

@Entity(
    tableName = "incidentes",
    indices = [
        Index(value = ["estudiante_id"]),
        Index(value = ["fecha_hora"]),
        Index(value = ["uuid"], unique = true),
        Index(value = ["sync_status"]),
        Index(value = ["is_deleted"])
    ]
)
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "estudiante_id") val studentId: Long,
    @ColumnInfo(name = "tipo") val type: String,
    @ColumnInfo(name = "severidad") val severity: String,
    @ColumnInfo(name = "descripcion") val description: String,
    @ColumnInfo(name = "fecha_hora") val dateTime: String,
    @ColumnInfo(name = "docente") val teacherName: String? = null,
    @ColumnInfo(name = "enviado") val sent: Boolean = false,

    // ----- Compatibilidad transición -----
    @ColumnInfo(name = "sincronizacion_pendiente") val syncPending: Boolean = true,

    // ----- Offline First v6 -----
    // Para incidentes mobile = PULL only. UUID se usa solo para idempotencia
    // al recibir registros del servidor (sin duplicar).
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = SyncState.Stored.IDLE,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    @ColumnInfo(name = "last_sync_attempt") val lastSyncAttempt: Long? = null,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,

    // Marca si el incidente fue creado localmente (no proviene del servidor).
    // Los locales NUNCA se sincronizan a Supabase desde mobile (decisión equipo).
    @ColumnInfo(name = "local_only") val localOnly: Boolean = true
)
