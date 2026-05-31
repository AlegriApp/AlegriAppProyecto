package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Respuesta REST de la tabla `incidentes` (PostgREST / Supabase).
 *
 * **PULL only:** mobile NO escribe en esta tabla. La inserción la realiza un
 * proceso externo a la app (decisión del equipo en Fase 0).
 */
data class IncidenteRemoteDto(
    val id: Long,
    val uuid: String? = null,
    @SerializedName("estudiante_id") val estudianteId: Long,
    @SerializedName("tipo_incidente_id") val tipoIncidenteId: Long,
    val descripcion: String,
    @SerializedName("fecha_hora") val fechaHora: String,
    @SerializedName("nivel_gravedad") val nivelGravedad: String,
    val estado: String,
    val observaciones: String? = null,
    @SerializedName("reportado_por_id") val reportadoPorId: Long? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
)
