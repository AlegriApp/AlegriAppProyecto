package com.example.myapplication.domain.usecase.incidents

import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.model.telegram.TelegramSendOutcome
import com.example.myapplication.domain.repository.IncidentRepository
import com.example.myapplication.domain.usecase.telegram.SendTelegramMessageUseCase
import com.example.myapplication.services.telegram.TelegramMessageBuilder

class SendIncidentReportUseCase(
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase,
    private val incidentRepository: IncidentRepository,
    private val defaultChatId: String
) {
    suspend operator fun invoke(student: Student, incident: Incident): TelegramSendOutcome {
        val message = TelegramMessageBuilder.buildIncidentReport(
            student = student,
            incident = incident
        )
        val chatId = student.telegramChatId?.takeIf { it.isNotBlank() }
            ?: defaultChatId
        val outcome = sendTelegramMessageUseCase(chatId, message)
        if (outcome is TelegramSendOutcome.Success && incident.id != 0L) {
            incidentRepository.markIncidentAsSent(incident.id)
        }
        return outcome
    }
}
