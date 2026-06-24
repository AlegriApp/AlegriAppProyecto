package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.TelegramMessageRequest
import com.example.myapplication.data.remote.dto.TelegramResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TelegramApiService {
    @POST("sendMessage")
    suspend fun sendMessage(
        @Body request: TelegramMessageRequest
    ): TelegramResponse
}
