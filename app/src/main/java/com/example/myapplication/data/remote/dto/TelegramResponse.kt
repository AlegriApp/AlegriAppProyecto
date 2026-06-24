package com.example.myapplication.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TelegramResponse(
    val ok: Boolean,
    @SerializedName("error_code") val errorCode: Int? = null,
    val description: String? = null,
    val result: TelegramMessageResult? = null
)

data class TelegramMessageResult(
    @SerializedName("message_id") val messageId: Long? = null
)
