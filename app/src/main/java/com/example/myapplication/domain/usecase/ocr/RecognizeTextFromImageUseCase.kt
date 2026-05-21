package com.example.myapplication.domain.usecase.ocr

import android.net.Uri
import com.example.myapplication.domain.model.OcrTextResult
import com.example.myapplication.domain.repository.OcrRepository

class RecognizeTextFromImageUseCase(
    private val repository: OcrRepository
) {
    suspend operator fun invoke(imageUri: Uri): Result<OcrTextResult> =
        repository.recognizeText(imageUri)
}
