package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myapplication.domain.model.sync.SyncState

@Entity(
    tableName = "asistencias",
    indices = [
        Index(value = ["estudiante_id"]),
        Index(value = ["curso_id"]),
        Index(value = ["materia_id"]),
        Index(value = ["fecha"]),
        Index(
            value = ["estudiante_id", "curso_id", "fecha", "materia_id"],
            unique = true
        ),
        Index(value = ["uuid"], unique = true),
        Index(value = ["sync_status"]),
        Index(value = ["is_deleted"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "estudiante_id") val studentId: Long,
    @ColumnInfo(name = "curso_id") val courseId: Long? = null,
    @ColumnInfo(name = "materia_id") val subjectId: Long? = null,
    @ColumnInfo(name = "docente_id") val teacherId: Long? = null,
    @ColumnInfo(name = "fecha") val date: String,
    @ColumnInfo(name = "hora_entrada") val entryTime: String? = null,
    @ColumnInfo(name = "estado") val status: String,
    @ColumnInfo(name = "observacion") val observation: String? = null,
    @ColumnInfo(name = "justificacion") val justification: String? = null,

    // ----- Compatibilidad transición (mantener mientras código viejo la consulte) -----
    @ColumnInfo(name = "sincronizacion_pendiente") val syncPending: Boolean = false,

    // ----- Offline First v6 -----
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = SyncState.Stored.IDLE,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    @ColumnInfo(name = "last_sync_attempt") val lastSyncAttempt: Long? = null,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
