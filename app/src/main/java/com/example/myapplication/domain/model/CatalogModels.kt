package com.example.myapplication.domain.model

data class CourseCatalog(
    val id: Long,
    val nombre: String,
    val displayName: String
)

data class SubjectCatalog(
    val id: Long,
    val nombre: String,
    val courseId: Long
)

data class EvaluationTypeCatalog(
    val id: Long,
    val nombre: String
)

data class AcademicPeriodCatalog(
    val id: Long,
    val nombre: String
)

data class IncidentTypeCatalog(
    val id: Long,
    val nombre: String
)

data class TelegramDestination(
    val chatId: String,
    val botToken: String?
)
