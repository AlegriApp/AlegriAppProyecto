package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.StudentAttendanceRecord
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    fun observeAttendanceByDate(date: String): Flow<List<StudentAttendanceRecord>>
    fun observeAttendanceByDateAndCourse(date: String, courseId: Long): Flow<List<StudentAttendanceRecord>>
    fun observeAttendanceByDateCourseAndSubject(
        date: String,
        courseId: Long,
        subjectId: Long
    ): Flow<List<StudentAttendanceRecord>>
    suspend fun upsertAttendance(attendance: Attendance)
}
