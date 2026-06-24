package com.example.myapplication.data.mapper

import com.example.myapplication.core.common.newUuid
import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.model.sync.SyncState

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

fun Grade.toEntity(
    existing: GradeEntity? = null,
    markPending: Boolean = true
): GradeEntity = GradeEntity(
    id = existing?.id ?: id,
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
    syncPending = markPending,
    subjectName = subject,
    periodName = period,
    evaluationTypeName = activityType,
    uuid = existing?.uuid ?: newUuid(),
    remoteId = existing?.remoteId,
    syncStatus = if (markPending) SyncState.Stored.IDLE else SyncState.Stored.SUCCESS,
    syncError = if (markPending) null else existing?.syncError,
    lastSyncAttempt = existing?.lastSyncAttempt,
    serverUpdatedAt = existing?.serverUpdatedAt,
    isDeleted = existing?.isDeleted ?: false
)

