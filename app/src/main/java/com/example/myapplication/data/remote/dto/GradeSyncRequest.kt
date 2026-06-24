package com.example.myapplication.data.remote.dto

data class GradeSyncRequest(
    val estudianteId: Long,
    val cursoId: Long,
    val materiaId: Long,
    val periodoAcademicoId: Long?,
    val docenteId: Long?,
    val calificaciones: List<GradeItem>,
    val promedio: Double,
    val syncMetadata: SyncMetadata = SyncMetadata()
) {
    data class GradeItem(
        val calificacionId: Long,
        val tipoEvaluacionId: Long,
        val descripcion: String? = null,
        val notaObtenida: Double,
        val notaMaxima: Double,
        val observacion: String? = null,
        val estado: String = "registrado"
    )

    data class SyncMetadata(
        val syncedAt: Long = System.currentTimeMillis(),
        val source: String = "mobile_offline_queue"
    )
}
