package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Respuesta REST de la tabla `incidentes` (PostgREST / Supabase). PULL y PUSH.
 *
 * **Cambio Fase 14:** mobile ahora también escribe en esta tabla (revirtió la
 * decisión inicial de PULL only).
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

/**
 * Body para upsert en `incidentes` desde mobile. Idempotente por `uuid`.
 *
 * Campos CHECK del servidor:
 *   - `nivel_gravedad` ∈ ('bajo','medio','alto','critico')
 *   - `estado` ∈ ('abierto','en_seguimiento','cerrado','archivado')
 *
 * `tipo_incidente_id` es FK obligatoria a `tipos_incidente`. El mapper resuelve
 * el id a partir del enum local; si no hay match, usa el default configurable.
 *
 * `reportado_por_id` es opcional; se llena con el id del docente cuando exista
 * sesión real, por ahora se envía null (lo que el servidor acepta).
 */
data class IncidenteInsertDto(
    val uuid: String,
    @SerializedName("estudiante_id") val estudianteId: Long,
    @SerializedName("tipo_incidente_id") val tipoIncidenteId: Long,
    val descripcion: String,
    @SerializedName("fecha_hora") val fechaHora: String,
    @SerializedName("nivel_gravedad") val nivelGravedad: String,
    val estado: String = "abierto",
    val observaciones: String? = null,
    @SerializedName("reportado_por_id") val reportadoPorId: Long? = null
)
