package com.example.myapplication.data.local.entity

data class AttendanceEntity(
    val id: Long = 0L,
    val studentId: Long,
    val date: String,
    val status: String,
    val synced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
