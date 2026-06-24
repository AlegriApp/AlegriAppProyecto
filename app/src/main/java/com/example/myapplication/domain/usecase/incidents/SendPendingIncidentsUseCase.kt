package com.example.myapplication.domain.usecase.incidents

import com.example.myapplication.domain.model.telegram.TelegramSendOutcome
import com.example.myapplication.domain.repository.IncidentRepository
import com.example.myapplication.domain.repository.StudentRepository

data class SendPendingIncidentsResult(
    val total: Int,
    val sent: Int,
    val failed: Int
) {
    val hasWork: Boolean get() = total > 0
}

/**
 * Recorre los incidentes locales con `sent = 0` y los envía por Telegram.
 *
 * - Si Telegram responde OK → marca el incidente como `sent = 1` (Room).
 * - Si Telegram falla → deja el incidente como pendiente para reintento futuro.
 * - Si el estudiante asociado no existe localmente → cuenta como fallo
 *   (no se puede construir el mensaje sin sus datos).
 *
 * El `onProgress(current, total)` se invoca antes de procesar cada incidente
 * y una última vez al terminar. Se usa desde [com.example.myapplication.core.sync.SyncWorker]
 * para actualizar la notificación foreground del sistema.
 */
class SendPendingIncidentsUseCase(
    private val incidentRepository: IncidentRepository,
    private val studentRepository: StudentRepository,
    private val sendIncidentReportUseCase: SendIncidentReportUseCase
) {
    suspend operator fun invoke(
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> }
    ): SendPendingIncidentsResult {
        val pending = incidentRepository.getPendingTelegramSend()
        if (pending.isEmpty()) {
            return SendPendingIncidentsResult(total = 0, sent = 0, failed = 0)
        }

        var sentCount = 0
        var failedCount = 0
        val total = pending.size

        pending.forEachIndexed { index, incident ->
            onProgress(index, total)

            val student = studentRepository.findById(incident.studentId)
            if (student == null) {
                failedCount++
                return@forEachIndexed
            }

            when (sendIncidentReportUseCase(student, incident)) {
                is TelegramSendOutcome.Success -> {
                    sentCount++
                    // markIncidentAsSent ya lo hace SendIncidentReportUseCase tras success,
                    // pero solo si el id != 0L. Aquí estamos seguros de tener ids reales.
                    if (incident.id != 0L) {
                        incidentRepository.markIncidentAsSent(incident.id)
                    }
                }
                is TelegramSendOutcome.Failure -> failedCount++
            }
        }

        // Empuje final para que la UI marque 100%.
        onProgress(total, total)

        return SendPendingIncidentsResult(
            total = total,
            sent = sentCount,
            failed = failedCount
        )
    }
}
