package com.example.myapplication.domain.usecase.auth

import com.example.myapplication.core.common.ResultState
import com.example.myapplication.domain.model.AuthSession
import com.example.myapplication.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): ResultState<AuthSession> =
        authRepository.login(email = email, password = password)
}
