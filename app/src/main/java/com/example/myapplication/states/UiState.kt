package com.example.myapplication.states

sealed class UiState {

    object Loading : UiState()

    data class Success(val data: String) : UiState()

    data class Error(val message: String) : UiState()
}