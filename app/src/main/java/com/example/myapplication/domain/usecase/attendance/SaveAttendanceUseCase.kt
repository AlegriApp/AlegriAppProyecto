package com.example.myapplication.domain.usecase.attendance

import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.repository.AttendanceRepository

class SaveAttendanceUseCase(
    private val repository: AttendanceRepository
) {
    suspend operator fun invoke(attendance: Attendance) {
        repository.upsertAttendance(attendance)
    }
}
