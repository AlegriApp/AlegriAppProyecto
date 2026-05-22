package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incidentes",
    indices = [
        Index(value = ["estudiante_id"]),
        Index(value = ["fecha_hora"])
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
    @ColumnInfo(name = "sincronizacion_pendiente") val syncPending: Boolean = true
)
