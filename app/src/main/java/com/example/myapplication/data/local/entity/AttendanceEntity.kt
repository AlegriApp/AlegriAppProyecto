package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asistencias",
    indices = [
        Index(value = ["estudiante_id"]),
        Index(value = ["curso_id"]),
        Index(value = ["fecha"])
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
    @ColumnInfo(name = "sincronizacion_pendiente") val syncPending: Boolean = false
)
