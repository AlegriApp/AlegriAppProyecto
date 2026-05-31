package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.telegram.TelegramSendOutcome

interface TelegramRepository {
    suspend fun sendMessage(
        chatId: String,
        message: String,
        botToken: String? = null
    ): TelegramSendOutcome
}
