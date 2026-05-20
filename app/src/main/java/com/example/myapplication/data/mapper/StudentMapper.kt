package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.StudentEntity
import com.example.myapplication.domain.model.Student

fun StudentEntity.toDomain(): Student = Student(
    id = id,
    fullName = fullName,
    grade = grade,
    section = section,
    representativeName = representativeName,
    telegramChatId = telegramChatId
)

fun Student.toEntity(): StudentEntity = StudentEntity(
    id = id,
    fullName = fullName,
    grade = grade,
    section = section,
    representativeName = representativeName,
    telegramChatId = telegramChatId
)

fun List<StudentEntity>.toDomainList(): List<Student> = map(StudentEntity::toDomain)
fun List<Student>.toEntityList(): List<StudentEntity> = map(Student::toEntity)
