package com.example.myapplication.core.navigation

object AppRoutes {
    const val Grades = "grades"
    const val GradeDetail = "grades_detail/{studentId}"

    fun gradeDetail(studentId: Long): String = "grades_detail/$studentId"
}
