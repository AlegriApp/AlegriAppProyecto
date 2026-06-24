package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Respuesta REST de la tabla `estudiantes` (PostgREST / Supabase). */
data class EstudianteRemoteDto(
    val id: Long,
    val uuid: String? = null,
    @SerializedName("codigo_institucional") val codigoInstitucional: String? = null,
    val nombre: String,
    val apellido: String,
    val estado: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null,
    @SerializedName("estudiante_curso") val estudianteCurso: List<EstudianteCursoRemoteDto>? = null,
    @SerializedName("estudiante_representante") val estudianteRepresentante: List<EstudianteRepresentanteRemoteDto>? = null
)

data class EstudianteCursoRemoteDto(
    @SerializedName("curso_id") val cursoId: Long? = null,
    val estado: String? = null,
    val cursos: CursoRemoteDto? = null
)

data class CursoRemoteDto(
    val id: Long? = null,
    val nombre: String? = null,
    val paralelo: String? = null,
    @SerializedName("niveles_academicos") val nivelAcademico: NivelAcademicoRemoteDto? = null
)

data class NivelAcademicoRemoteDto(
    val nombre: String? = null
)
