package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: Long,
    val fullName: String,
    val grade: String,
    val section: String,
    val representativeName: String,
    val telegramChatId: String?
)
