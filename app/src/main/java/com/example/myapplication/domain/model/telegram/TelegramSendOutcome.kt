package com.example.myapplication.domain.model.telegram

sealed class TelegramSendOutcome {
    data object Success : TelegramSendOutcome()
    data class Failure(
        val message: String,
        val type: FailureType = FailureType.Unknown
    ) : TelegramSendOutcome()

    enum class FailureType {
        MissingConfiguration,
        Network,
        Api,
        MessageTooLong,
        Unknown
    }
}
