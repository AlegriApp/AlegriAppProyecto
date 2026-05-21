package com.example.myapplication.domain.repository

import android.net.Uri
import com.example.myapplication.domain.model.OcrTextResult

interface OcrRepository {
    suspend fun recognizeText(imageUri: Uri): Result<OcrTextResult>
}
