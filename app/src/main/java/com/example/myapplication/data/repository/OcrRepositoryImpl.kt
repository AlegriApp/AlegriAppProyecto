package com.example.myapplication.data.repository

import android.net.Uri
import com.example.myapplication.domain.model.OcrTextResult
import com.example.myapplication.domain.repository.OcrRepository
import com.example.myapplication.services.mlkit.TextRecognitionProcessor

class OcrRepositoryImpl(
    private val processor: TextRecognitionProcessor
) : OcrRepository {
    override suspend fun recognizeText(imageUri: Uri): Result<OcrTextResult> =
        processor.processImage(imageUri).map { text ->
            OcrTextResult(rawText = text)
        }
}
