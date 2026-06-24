package com.example.myapplication.domain.model

data class AuthSession(
    val userId: Long,
    val fullName: String,
    val email: String,
    val roleName: String? = null
)
