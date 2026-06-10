package com.example.myapplication.domain.repository

import com.example.myapplication.core.common.ResultState
import com.example.myapplication.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeSession(): Flow<AuthSession?>
    suspend fun login(email: String, password: String): ResultState<AuthSession>
    suspend fun logout()
}
