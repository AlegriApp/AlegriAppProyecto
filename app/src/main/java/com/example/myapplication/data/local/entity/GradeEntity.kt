package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val studentId: Long,
    val subject: String,
    val period: String,
    val activityName: String,
    val activityType: String,
    val score: Double,
    val maxScore: Double,
    val synced: Boolean = false
)
