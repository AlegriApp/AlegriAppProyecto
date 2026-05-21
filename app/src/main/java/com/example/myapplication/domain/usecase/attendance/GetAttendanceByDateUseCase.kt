package com.example.myapplication.domain.usecase.attendance

import com.example.myapplication.domain.model.StudentAttendanceRecord
import com.example.myapplication.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow

class GetAttendanceByDateUseCase(
    private val repository: AttendanceRepository
) {
    operator fun invoke(date: String): Flow<List<StudentAttendanceRecord>> =
        repository.observeAttendanceByDate(date)
}
