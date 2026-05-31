package com.example.myapplication.data.remote.api

import com.example.myapplication.services.telegram.TelegramConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TelegramApiFactory {
    private val clients = mutableMapOf<String, TelegramApiService>()

    fun getService(botToken: String, httpClient: OkHttpClient): TelegramApiService {
        val key = botToken.ifBlank { "_default_" }
        return clients.getOrPut(key) {
            Retrofit.Builder()
                .baseUrl(TelegramConfig.botBaseUrl(botToken))
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TelegramApiService::class.java)
        }
    }
}
