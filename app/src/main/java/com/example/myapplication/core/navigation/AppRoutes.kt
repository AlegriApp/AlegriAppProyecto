package com.example.myapplication.core.navigation

object AppRoutes {
    const val Home = "home"
    const val Attendance = "attendance"
    const val Grades = "grades"
    const val Incidents = "incidents"
    const val GradeDetail = "grades_detail/{studentId}"

    fun gradeDetail(studentId: Long): String = "grades_detail/$studentId"
}
