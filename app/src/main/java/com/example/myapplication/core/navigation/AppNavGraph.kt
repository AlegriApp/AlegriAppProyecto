package com.example.myapplication.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.presentation.grades.GradeDetailScreen
import com.example.myapplication.presentation.grades.GradesScreenRoute

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Grades
    ) {
        composable(AppRoutes.Grades) {
            GradesScreenRoute(
                onBack = { navController.popBackStack() },
                onOpenDetail = { studentId, _, _ ->
                    navController.navigate(AppRoutes.gradeDetail(studentId))
                }
            )
        }
        composable(
            route = AppRoutes.GradeDetail,
            arguments = listOf(navArgument("studentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getLong("studentId") ?: 0L
            GradeDetailScreen(
                studentId = studentId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
