package com.example.myapplication.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.common.ResultState
import com.example.myapplication.domain.repository.AuthRepository
import com.example.myapplication.domain.usecase.auth.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        currentUser = session,
                        isLoggedIn = session != null
                    )
                }
            }
        }
    }

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun submit() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        val emailError = when {
            email.isBlank() -> "Ingresa tu correo."
            !email.contains('@') -> "Ingresa un correo válido."
            else -> null
        }
        val passwordError = when {
            password.isBlank() -> "Ingresa tu contraseña."
            password.length < 4 -> "La contraseña debe tener al menos 4 caracteres."
            else -> null
        }

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginUseCase(email = email, password = password)) {
                is ResultState.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = result.data,
                            isLoggedIn = true,
                            password = ""
                        )
                    }
                }
                is ResultState.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            errorMessage = result.message
                        )
                    }
                }
                ResultState.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}
