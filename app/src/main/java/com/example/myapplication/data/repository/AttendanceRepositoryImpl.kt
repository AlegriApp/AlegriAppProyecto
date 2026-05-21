package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.AttendanceDao
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.mapper.toEntity
import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.StudentAttendanceRecord
import com.example.myapplication.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AttendanceRepositoryImpl(
    private val attendanceDao: AttendanceDao,
    private val studentDao: StudentDao
) : AttendanceRepository {

    override fun observeAttendanceByDate(date: String): Flow<List<StudentAttendanceRecord>> =
        combine(
            studentDao.observeStudents(),
            attendanceDao.observeAttendanceByDate(date)
        ) { students, attendance ->
            val attendanceByStudent = attendance.associateBy { it.studentId }
            students.map { student ->
                StudentAttendanceRecord(
                    student = student.toDomain(),
                    attendance = attendanceByStudent[student.id]?.toDomain()
                )
            }
        }

    override suspend fun upsertAttendance(attendance: Attendance) {
        attendanceDao.insertOrReplaceAttendance(attendance.toEntity())
    }
}
