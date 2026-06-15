package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CursoCatalogRemoteDto(
    val id: Long,
    val nombre: String,
    val paralelo: String? = null,
    @SerializedName("anio_lectivo") val anioLectivo: String? = null,
    @SerializedName("periodo_academico_id") val periodoAcademicoId: Long? = null,
    val estado: String? = null
)

data class MateriaCatalogRemoteDto(
    val id: Long,
    val nombre: String,
    @SerializedName("curso_id") val cursoId: Long,
    val estado: String? = null
)

data class TipoEvaluacionRemoteDto(
    val id: Long,
    val nombre: String,
    val activo: Boolean? = true
)

data class PeriodoAcademicoRemoteDto(
    val id: Long,
    val nombre: String,
    @SerializedName("anio_lectivo") val anioLectivo: String? = null,
    val activo: Boolean? = true
)

data class TipoIncidenteRemoteDto(
    val id: Long,
    val nombre: String,
    val activo: Boolean? = true
)

/** Fila directa de la tabla `estudiante_curso` (sync PULL). */
data class EstudianteCursoEnrollmentRemoteDto(
    @SerializedName("estudiante_id") val estudianteId: Long,
    @SerializedName("curso_id") val cursoId: Long,
    val estado: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
)

data class ConfiguracionTelegramRemoteDto(
    val id: Long = 0L,
    @SerializedName("chat_id") val chatId: String,
    @SerializedName("token_bot_encriptado") val tokenBot: String? = null,
    @SerializedName("representante_id") val representanteId: Long? = null,
    @SerializedName("estado_integracion") val estadoIntegracion: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
)

data class EstudianteRepresentanteRemoteDto(
    @SerializedName("es_principal") val esPrincipal: Boolean? = null,
    @SerializedName("representante_id") val representanteId: Long? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null,
    val representantes: RepresentanteRemoteDto? = null
)

data class RepresentanteRemoteDto(
    val id: Long,
    val nombre: String? = null,
    val apellido: String? = null,
    @SerializedName("configuracion_telegram") val configuracionTelegram: List<ConfiguracionTelegramRemoteDto>? = null
)

data class DocenteCursoRemoteDto(
    @SerializedName("docente_id") val docenteId: Long,
    @SerializedName("curso_id") val cursoId: Long,
    @SerializedName("materia_id") val materiaId: Long? = null,
    @SerializedName("es_tutor") val esTutor: Boolean? = null,
    val estado: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null,
    val cursos: CursoCatalogRemoteDto? = null
)
