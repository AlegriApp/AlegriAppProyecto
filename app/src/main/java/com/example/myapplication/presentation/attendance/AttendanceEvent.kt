package com.example.myapplication.presentation.attendance

sealed interface AttendanceEvent {
    data class LoadAttendance(val date: String, val gradeSection: String) : AttendanceEvent
    data class StatusSelected(val studentId: Long, val status: AttendanceStatus) : AttendanceEvent
    data object SaveAttendance : AttendanceEvent
    data object SendReport : AttendanceEvent
}
