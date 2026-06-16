package com.example.myapplication.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.myapplication.core.di.AppModule
import com.example.myapplication.presentation.attendance.AttendanceScreenRoute
import com.example.myapplication.presentation.grades.GradeDetailScreen
import com.example.myapplication.presentation.grades.GradesScreenRoute
import com.example.myapplication.presentation.home.HomeScreen
import com.example.myapplication.presentation.incidents.IncidentScreenRoute
import com.example.myapplication.presentation.login.LoginScreenRoute

@Composable
fun AppNavGraph() {
    val context = LocalContext.current
    val authRepository = AppModule.provideAuthRepository(context)
    val session by authRepository.observeSession().collectAsStateWithLifecycle(initialValue = null)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route.orEmpty()

    LaunchedEffect(session, currentRoute) {
        when {
            session != null && currentRoute.contains("Login") -> {
                navController.navigate(Home) {
                    popUpTo<Login> { inclusive = true }
                    launchSingleTop = true
                }
            }

            session == null && currentRoute.isNotBlank() && !currentRoute.contains("Login") -> {
                navController.navigate(Login) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (session != null) Home else Login
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
            HomeScreen(
                onOpenAttendance = { navController.navigate(Attendance) },
                onOpenGrades = { navController.navigate(Grades) },
                onOpenIncidents = { navController.navigate(Incidents) }
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
