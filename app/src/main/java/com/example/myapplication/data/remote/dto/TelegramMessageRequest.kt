package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TelegramMessageRequest(
    @SerializedName("chat_id") val chatId: String,
    val text: String,
    @SerializedName("parse_mode") val parseMode: String = "Markdown"
)
