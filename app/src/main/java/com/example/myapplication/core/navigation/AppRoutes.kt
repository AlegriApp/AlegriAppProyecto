package com.example.myapplication.core.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute

@Serializable
data object Login : AppRoute

@Serializable
data object Home : AppRoute

@Serializable
data object Attendance : AppRoute

@Serializable
data object Grades : AppRoute

@Serializable
data object Incidents : AppRoute

@Serializable
data class GradeDetail(
    val studentId: Long
) : AppRoute
