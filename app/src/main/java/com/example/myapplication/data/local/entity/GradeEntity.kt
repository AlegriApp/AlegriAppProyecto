package com.example.myapplication.data.local.entity

data class GradeEntity(
    val id: Long = 0L,
    val studentId: Long,
    val subject: String,
    val period: String,
    val activity: String,
    val score: Double? = null,
    val synced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
