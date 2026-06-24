package com.example.myapplication.services.telegram

object TelegramConfig {
    const val TELEGRAM_API_HOST = "https://api.telegram.org/"

    /** Base URL de Retrofit: el token va en la ruta, no como @Path (el ':' rompe la URL). */
    fun botBaseUrl(botToken: String): String =
        "${TELEGRAM_API_HOST}bot$botToken/"
}
