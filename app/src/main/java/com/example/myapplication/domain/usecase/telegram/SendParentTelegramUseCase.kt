package com.example.myapplication.domain.usecase.telegram

import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.model.telegram.TelegramSendOutcome
import com.example.myapplication.domain.repository.CatalogRepository

/**
 * Envía un mensaje al representante del estudiante usando `configuracion_telegram`
 * (vía [CatalogRepository.resolveTelegramForStudent]), con fallback a chat/token por defecto.
 */
class SendParentTelegramUseCase(
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase,
    private val catalogRepository: CatalogRepository,
    private val defaultChatId: String,
    private val defaultBotToken: String
) {
    suspend operator fun invoke(student: Student, message: String): TelegramSendOutcome {
        val telegram = catalogRepository.resolveTelegramForStudent(student.id)
        val chatId = telegram?.chatId?.takeIf { it.isNotBlank() }
            ?: student.telegramChatId?.takeIf { it.isNotBlank() }
            ?: defaultChatId
        val botToken = telegram?.botToken?.takeIf { it.isNotBlank() }
            ?: defaultBotToken.takeIf { it.isNotBlank() }
        return sendTelegramMessageUseCase(chatId, message, botToken)
    }
}
