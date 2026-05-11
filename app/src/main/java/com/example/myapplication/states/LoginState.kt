package com.example.myapplication.states

sealed class LoginState {

    object Idle : LoginState()

    object Loading : LoginState()

    data class Success(val user: String) : LoginState()

    data class Error(val message: String) : LoginState()
}