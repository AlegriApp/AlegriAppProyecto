package com.example.myapplication.presentation.grades

sealed interface GradesEvent {
    data object LoadData : GradesEvent
    data class SubjectSelected(val subject: String) : GradesEvent
    data class PeriodSelected(val period: String) : GradesEvent
    data class EditGrade(
        val studentId: Long,
        val activityName: String,
        val activityType: String,
        val score: Double,
        val maxScore: Double = 20.0
    ) : GradesEvent
    data object RefreshAverages : GradesEvent
    data class OpenDetail(val studentId: Long) : GradesEvent
    data object SaveGrades : GradesEvent
    data object SendBulletinClicked : GradesEvent
    data object ClearMessages : GradesEvent
}
