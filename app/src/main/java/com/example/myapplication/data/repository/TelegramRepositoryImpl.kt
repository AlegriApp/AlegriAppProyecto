package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.api.TelegramApiFactory
import com.example.myapplication.data.remote.api.TelegramApiService
import com.example.myapplication.data.remote.dto.TelegramMessageRequest
import com.example.myapplication.domain.model.telegram.TelegramSendOutcome
import com.example.myapplication.domain.repository.TelegramRepository
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class TelegramRepositoryImpl(
    private val defaultApi: TelegramApiService,
    private val defaultBotToken: String,
    private val httpClient: OkHttpClient
) : TelegramRepository {

    override suspend fun sendMessage(
        chatId: String,
        message: String,
        botToken: String?
    ): TelegramSendOutcome {
        val token = botToken?.takeIf { it.isNotBlank() } ?: defaultBotToken
        val api = if (token == defaultBotToken) {
            defaultApi
        } else {
            TelegramApiFactory.getService(token, httpClient)
        }
        when {
            token.isBlank() -> return TelegramSendOutcome.Failure(
                message = "Token del bot no configurado. Agrega TELEGRAM_BOT_TOKEN en local.properties y recompila.",
                type = TelegramSendOutcome.FailureType.MissingConfiguration
            )
            chatId.isBlank() -> return TelegramSendOutcome.Failure(
                message = "Chat ID no configurado. Agrega TELEGRAM_CHAT_ID en local.properties y recompila.",
                type = TelegramSendOutcome.FailureType.MissingConfiguration
            )
            message.length > MAX_MESSAGE_LENGTH -> return TelegramSendOutcome.Failure(
                message = "El mensaje supera el límite de Telegram ($MAX_MESSAGE_LENGTH caracteres).",
                type = TelegramSendOutcome.FailureType.MessageTooLong
            )
        }

        return try {
            val response = api.sendMessage(
                request = TelegramMessageRequest(chatId = chatId, text = message)
            )
            if (response.ok) {
                TelegramSendOutcome.Success
            } else {
                TelegramSendOutcome.Failure(
                    message = mapApiError(response.errorCode, response.description),
                    type = TelegramSendOutcome.FailureType.Api
                )
            }
        } catch (error: IOException) {
            TelegramSendOutcome.Failure(
                message = mapNetworkError(error),
                type = TelegramSendOutcome.FailureType.Network
            )
        } catch (error: Exception) {
            TelegramSendOutcome.Failure(
                message = error.message ?: "Error inesperado al enviar por Telegram.",
                type = TelegramSendOutcome.FailureType.Unknown
            )
        }
    }

    private fun mapApiError(code: Int?, description: String?): String {
        val detail = description?.trim().orEmpty()
        return when (code) {
            400 -> if (detail.isNotBlank()) "Solicitud inválida: $detail" else "Solicitud inválida a Telegram."
            401 -> "Token del bot inválido o revocado."
            403 -> "El bot no tiene permiso para enviar al chat indicado."
            404 -> "Chat no encontrado. Verifica TELEGRAM_CHAT_ID."
            429 -> "Demasiados envíos. Intenta de nuevo en unos minutos."
            else -> if (detail.isNotBlank()) "Telegram respondió con error: $detail" else "Telegram rechazó el envío."
        }
    }

    private fun mapNetworkError(error: IOException): String = when (error) {
        is UnknownHostException -> "Sin conexión a internet o no se pudo resolver el servidor."
        is SocketTimeoutException -> "Tiempo de espera agotado al contactar Telegram."
        else -> error.message?.let { "Error de red: $it" } ?: "Error de red al enviar por Telegram."
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 4096
    }
}
