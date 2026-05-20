package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.domain.model.Grade

fun GradeEntity.toDomain(): Grade = Grade(
    id = id,
    studentId = studentId,
    subject = subject,
    period = period,
    activityName = activityName,
    activityType = activityType,
    score = score,
    maxScore = maxScore,
    synced = synced
)

fun Grade.toEntity(): GradeEntity = GradeEntity(
    id = id,
    studentId = studentId,
    subject = subject,
    period = period,
    activityName = activityName,
    activityType = activityType,
    score = score,
    maxScore = maxScore,
    synced = synced
)
