package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Inserción / upsert en tabla `calificaciones` (alegriapp_create_tables_og.txt). */
data class CalificacionInsertDto(
    val uuid: String,
    @SerializedName("estudiante_id") val estudianteId: Long,
    @SerializedName("materia_id") val materiaId: Long,
    @SerializedName("curso_id") val cursoId: Long,
    @SerializedName("periodo_academico_id") val periodoAcademicoId: Long? = null,
    @SerializedName("tipo_evaluacion_id") val tipoEvaluacionId: Long,
    val descripcion: String? = null,
    @SerializedName("nota_obtenida") val notaObtenida: Double,
    @SerializedName("nota_maxima") val notaMaxima: Double,
    val observacion: String? = null,
    @SerializedName("docente_id") val docenteId: Long? = null,
    val estado: String = "registrado"
)

data class CalificacionRemoteResponseDto(
    val id: Long,
    val uuid: String? = null,
    @SerializedName("estudiante_id") val estudianteId: Long,
    @SerializedName("nota_obtenida") val notaObtenida: Double,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("deleted_at") val deletedAt: String? = null
)
