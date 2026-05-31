package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.myapplication.domain.model.sync.SyncState

@Entity(
    tableName = "calificaciones",
    indices = [
        Index(value = ["estudiante_id"]),
        Index(value = ["materia_id"]),
        Index(value = ["curso_id"]),
        Index(value = ["periodo_academico_id"]),
        Index(
            value = ["estudiante_id", "materia_nombre", "periodo_nombre", "descripcion"],
            unique = true
        ),
        Index(value = ["uuid"], unique = true),
        Index(value = ["sync_status"]),
        Index(value = ["is_deleted"])
    ]
)
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "estudiante_id") val studentId: Long,
    @ColumnInfo(name = "materia_id") val subjectId: Long? = null,
    @ColumnInfo(name = "curso_id") val courseId: Long? = null,
    @ColumnInfo(name = "periodo_academico_id") val periodAcademicId: Long? = null,
    @ColumnInfo(name = "tipo_evaluacion_id") val evaluationTypeId: Long? = null,
    @ColumnInfo(name = "descripcion") val description: String,
    @ColumnInfo(name = "nota_obtenida") val score: Double,
    @ColumnInfo(name = "nota_maxima") val maxScore: Double,
    @ColumnInfo(name = "observacion") val observation: String? = null,
    @ColumnInfo(name = "docente_id") val teacherId: Long? = null,
    @ColumnInfo(name = "estado") val state: String = "registrado",

    // ----- Compatibilidad transición -----
    @ColumnInfo(name = "sincronizacion_pendiente") val syncPending: Boolean = false,

    @ColumnInfo(name = "materia_nombre") val subjectName: String = "General",
    @ColumnInfo(name = "periodo_nombre") val periodName: String = "Actual",
    @ColumnInfo(name = "tipo_evaluacion_nombre") val evaluationTypeName: String = "General",

    // ----- Offline First v6 -----
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "remote_id") val remoteId: Long? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String = SyncState.Stored.IDLE,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
    @ColumnInfo(name = "last_sync_attempt") val lastSyncAttempt: Long? = null,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)
