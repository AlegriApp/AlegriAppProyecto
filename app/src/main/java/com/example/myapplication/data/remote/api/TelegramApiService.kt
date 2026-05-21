package com.example.myapplication.data.remote.api

import com.example.myapplication.data.remote.dto.TelegramMessageRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TelegramApiService {
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Body request: TelegramMessageRequest
    )
}
