package com.example.myapplication.data.remote.dto

data class AttendanceSyncRequest(
    val fecha: String,
    val cursoId: Long,
    val materiaId: Long?,
    val docenteId: Long?,
    val asistencias: List<Record>,
    val syncMetadata: SyncMetadata = SyncMetadata()
) {
    data class Record(
        val asistenciaId: Long,
        val estudianteId: Long,
        val estado: String,
        val observacion: String? = null,
        val justificacion: String? = null,
        val sincronizacionPendiente: Boolean = true
    )

    data class SyncMetadata(
        val requestedAt: Long = System.currentTimeMillis(),
        val source: String = "mobile_offline_queue"
    )
}
