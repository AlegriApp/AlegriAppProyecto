package com.example.myapplication.domain.model

data class Student(
    val id: Long,
    val fullName: String,
    val grade: String,
    val section: String,
    val representativeName: String,
    val telegramChatId: String? = null,
    val aliases: List<String> = emptyList()
)
