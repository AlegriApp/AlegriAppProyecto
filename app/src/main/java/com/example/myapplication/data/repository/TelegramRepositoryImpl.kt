package com.example.myapplication.data.repository

import com.example.myapplication.data.remote.api.TelegramApiService
import com.example.myapplication.data.remote.dto.TelegramMessageRequest
import com.example.myapplication.domain.repository.TelegramRepository

class TelegramRepositoryImpl(
    private val api: TelegramApiService,
    private val botToken: String
) : TelegramRepository {
    override suspend fun sendMessage(chatId: String, message: String): Boolean {
        if (botToken.isBlank() || chatId.isBlank()) return false
        return runCatching {
            api.sendMessage(
                token = botToken,
                request = TelegramMessageRequest(chatId = chatId, text = message)
            )
        }.isSuccess
    }
}
