package com.example.myapplication.presentation.attendance

import android.net.Uri
import com.example.myapplication.domain.model.AttendanceStatus

sealed interface AttendanceEvent {
    data object LoadStudents : AttendanceEvent
    data class ChangeDate(val selectedDate: String) : AttendanceEvent
    data class MarkPresent(val studentId: Long) : AttendanceEvent
    data class MarkLate(val studentId: Long) : AttendanceEvent
    data class MarkAbsent(val studentId: Long) : AttendanceEvent
    data object MarkAllPresent : AttendanceEvent
    data object ClearMarks : AttendanceEvent
    data object SaveAttendance : AttendanceEvent
    data object SendReport : AttendanceEvent
    data class OcrImageSelected(val uri: Uri) : AttendanceEvent
    data object ApplyOcrSuggestions : AttendanceEvent
    data object ClearMessages : AttendanceEvent
}
