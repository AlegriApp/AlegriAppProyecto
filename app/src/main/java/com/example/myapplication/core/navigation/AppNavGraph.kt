package com.example.myapplication.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.presentation.grades.GradeDetailPlaceholderScreen
import com.example.myapplication.presentation.grades.GradesScreenRoute

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    var selectedSubject by remember { mutableStateOf("Matemáticas") }
    var selectedPeriod by remember { mutableStateOf("1er Lapso") }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Grades
    ) {
        composable(AppRoutes.Grades) {
            GradesScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenDetail = { studentId, subject, period ->
                    selectedSubject = subject
                    selectedPeriod = period
                    navController.navigate(AppRoutes.gradeDetail(studentId))
                }
            )
        }
        composable(
            route = AppRoutes.GradeDetail,
            arguments = listOf(navArgument("studentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getLong("studentId") ?: 0L
            GradeDetailPlaceholderScreen(
                studentId = studentId,
                selectedSubject = selectedSubject,
                selectedPeriod = selectedPeriod,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
