package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.domain.model.Grade

fun GradeEntity.toDomain(): Grade = Grade(
    id = id,
    studentId = studentId,
    subjectId = subjectId,
    courseId = courseId,
    periodAcademicId = periodAcademicId,
    evaluationTypeId = evaluationTypeId,
    subject = subjectName,
    period = periodName,
    activityName = description,
    activityType = evaluationTypeName,
    score = score,
    maxScore = maxScore,
    observation = observation,
    teacherId = teacherId,
    state = state,
    syncPending = syncPending
)

fun Grade.toEntity(): GradeEntity = GradeEntity(
    id = id,
    studentId = studentId,
    subjectId = subjectId,
    courseId = courseId,
    periodAcademicId = periodAcademicId,
    evaluationTypeId = evaluationTypeId,
    description = activityName,
    score = score,
    maxScore = maxScore,
    observation = observation,
    teacherId = teacherId,
    state = state,
    syncPending = syncPending,
    subjectName = subject,
    periodName = period,
    evaluationTypeName = activityType
)
