package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.StudentEntity
import com.example.myapplication.domain.model.Student

fun StudentEntity.toDomain(): Student = Student(
    id = id,
    firstName = firstName,
    lastName = lastName,
    gradeSection = gradeSection,
    representativeName = representativeName,
    representativePhone = representativePhone,
    representativeChatId = representativeChatId,
    isActive = isActive
)

fun Student.toEntity(): StudentEntity = StudentEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    gradeSection = gradeSection,
    representativeName = representativeName,
    representativePhone = representativePhone,
    representativeChatId = representativeChatId,
    isActive = isActive
)
