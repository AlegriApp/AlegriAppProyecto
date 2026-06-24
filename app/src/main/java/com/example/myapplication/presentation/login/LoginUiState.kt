package com.example.myapplication.presentation.login

import com.example.myapplication.domain.model.AuthSession

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: AuthSession? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null
)
