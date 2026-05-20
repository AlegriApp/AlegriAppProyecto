package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.domain.model.Grade

fun GradeEntity.toDomain(): Grade = Grade(
    id = id,
    studentId = studentId,
    subject = subject,
    period = period,
    activity = activity,
    score = score,
    synced = synced,
    updatedAt = updatedAt
)

fun Grade.toEntity(): GradeEntity = GradeEntity(
    id = id,
    studentId = studentId,
    subject = subject,
    period = period,
    activity = activity,
    score = score,
    synced = synced,
    updatedAt = updatedAt
)
