package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Inserción en tabla `asistencias` (alegriapp_create_tables_og.txt). */
data class AsistenciaInsertDto(
    @SerializedName("estudiante_id") val estudianteId: Long,
    @SerializedName("curso_id") val cursoId: Long,
    @SerializedName("materia_id") val materiaId: Long? = null,
    val fecha: String,
    @SerializedName("hora_entrada") val horaEntrada: String? = null,
    val estado: String,
    val observacion: String? = null,
    val justificacion: String? = null,
    @SerializedName("docente_id") val docenteId: Long? = null,
    @SerializedName("sincronizacion_pendiente") val sincronizacionPendiente: Boolean = false
)

data class AsistenciaRemoteResponseDto(
    val id: Long,
    @SerializedName("estudiante_id") val estudianteId: Long,
    val fecha: String,
    val estado: String
)
