package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Inserción / upsert en tabla `asistencias` (alegriapp_create_tables_og.txt). */
data class AsistenciaInsertDto(
    val uuid: String,
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
    val uuid: String? = null,
    @SerializedName("estudiante_id") val estudianteId: Long,
    val fecha: String,
    val estado: String,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
)
