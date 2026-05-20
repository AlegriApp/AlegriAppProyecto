package com.example.myapplication.domain.model

data class Student(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val gradeSection: String,
    val representativeName: String? = null,
    val representativePhone: String? = null,
    val representativeChatId: String? = null,
    val isActive: Boolean = true
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}
