package com.example.myapplication.domain.usecase.telegram

import com.example.myapplication.domain.repository.TelegramRepository

class SendTelegramMessageUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: String, message: String): Boolean =
        repository.sendMessage(chatId, message)
}
