package com.example.myapplication.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.presentation.attendance.AttendanceScreenRoute
import com.example.myapplication.presentation.grades.GradeDetailScreen
import com.example.myapplication.presentation.grades.GradesScreenRoute
import com.example.myapplication.presentation.home.HomeScreenRoute
import com.example.myapplication.presentation.incidents.IncidentScreenRoute
import com.example.myapplication.presentation.login.LoginScreenRoute

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Login
    ) {
        composable<Login> {
            LoginScreenRoute(
                onLoginSuccess = {
                    navController.navigate(Home) {
                        popUpTo<Login> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Home> {
            HomeScreenRoute(
                onOpenAttendance = { navController.navigate(Attendance) },
                onOpenGrades = { navController.navigate(Grades) },
                onOpenIncidents = { navController.navigate(Incidents) },
                onLogout = {
                    navController.navigate(Login) {
                        popUpTo(Home) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Attendance> {
            AttendanceScreenRoute(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Grades> {
            GradesScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenDetail = { studentId, _, _ ->
                    navController.navigate(GradeDetail(studentId))
                }
            )
        }

        composable<Incidents> {
            IncidentScreenRoute(
                onBack = { navController.popBackStack() }
            )
        }

        composable<GradeDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<GradeDetail>()
            GradeDetailScreen(
                studentId = route.studentId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
