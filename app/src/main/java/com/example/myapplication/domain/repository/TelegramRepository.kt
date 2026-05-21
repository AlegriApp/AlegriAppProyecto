package com.example.myapplication.domain.repository

interface TelegramRepository {
    suspend fun sendMessage(chatId: String, message: String): Boolean
}
