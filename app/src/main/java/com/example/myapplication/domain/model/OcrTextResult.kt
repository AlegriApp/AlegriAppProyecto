package com.example.myapplication.domain.model

data class OcrTextResult(
    val rawText: String,
    val confidence: Float? = null
)
