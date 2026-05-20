package com.example.myapplication.data.local.entity

data class StudentEntity(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val gradeSection: String,
    val representativeName: String?,
    val representativePhone: String?,
    val representativeChatId: String?,
    val isActive: Boolean = true
)
