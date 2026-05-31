package com.example.myapplication.data.mapper

import com.example.myapplication.core.common.newUuid
import com.example.myapplication.data.local.entity.StudentEntity
import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.model.sync.SyncState

fun StudentEntity.toDomain(): Student = Student(
    id = id,
    fullName = fullName,
    grade = grade,
    section = section,
    representativeName = representativeName,
    telegramChatId = telegramChatId
)

fun Student.toEntity(existing: StudentEntity? = null): StudentEntity = StudentEntity(
    id = id,
    fullName = fullName,
    grade = grade,
    section = section,
    representativeName = representativeName,
    telegramChatId = telegramChatId,
    uuid = existing?.uuid ?: newUuid(),
    remoteId = existing?.remoteId,
    syncStatus = existing?.syncStatus ?: SyncState.Stored.SUCCESS,
    syncError = existing?.syncError,
    lastSyncAttempt = existing?.lastSyncAttempt,
    serverUpdatedAt = existing?.serverUpdatedAt,
    isDeleted = existing?.isDeleted ?: false
)

fun List<StudentEntity>.toDomainList(): List<Student> = map(StudentEntity::toDomain)
fun List<Student>.toEntityList(): List<StudentEntity> = map { it.toEntity() }
