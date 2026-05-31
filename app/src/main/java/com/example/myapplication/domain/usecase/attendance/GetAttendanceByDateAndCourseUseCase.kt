package com.example.myapplication.domain.usecase.attendance

import com.example.myapplication.domain.model.StudentAttendanceRecord
import com.example.myapplication.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow

class GetAttendanceByDateAndCourseUseCase(
    private val repository: AttendanceRepository
) {
    operator fun invoke(
        date: String,
        courseId: Long,
        subjectId: Long
    ): Flow<List<StudentAttendanceRecord>> =
        repository.observeAttendanceByDateCourseAndSubject(date, courseId, subjectId)
}
