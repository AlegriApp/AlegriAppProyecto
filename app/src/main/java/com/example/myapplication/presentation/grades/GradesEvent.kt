package com.example.myapplication.presentation.grades

sealed interface GradesEvent {
    data class SubjectSelected(val subject: String) : GradesEvent
    data class PeriodSelected(val period: String) : GradesEvent
    data class OpenDetail(val studentId: Long) : GradesEvent
    data object SendBulletinClicked : GradesEvent
}
