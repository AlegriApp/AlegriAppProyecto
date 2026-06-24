package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.AttendanceDao
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.mapper.toEntity
import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.AttendanceStatus
import com.example.myapplication.domain.model.StudentAttendanceRecord
import com.example.myapplication.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AttendanceRepositoryImpl(
    private val attendanceDao: AttendanceDao,
    private val studentDao: StudentDao
) : AttendanceRepository {

    override fun observeAttendanceByDate(date: String): Flow<List<StudentAttendanceRecord>> =
        buildAttendanceFlow(date, courseId = null, subjectId = null)

    override fun observeAttendanceByDateAndCourse(
        date: String,
        courseId: Long
    ): Flow<List<StudentAttendanceRecord>> = buildAttendanceFlow(date, courseId = courseId, subjectId = null)

    override fun observeAttendanceByDateCourseAndSubject(
        date: String,
        courseId: Long,
        subjectId: Long
    ): Flow<List<StudentAttendanceRecord>> =
        buildAttendanceFlow(date, courseId = courseId, subjectId = subjectId)

    private fun buildAttendanceFlow(
        date: String,
        courseId: Long?,
        subjectId: Long?
    ): Flow<List<StudentAttendanceRecord>> {
        val studentsFlow = if (courseId != null) {
            studentDao.observeStudentsByCourse(courseId)
        } else {
            studentDao.observeStudents()
        }
        val attendanceFlow = when {
            courseId != null && subjectId != null ->
                attendanceDao.observeAttendanceByDateCourseSubject(date, courseId, subjectId)
            else -> attendanceDao.observeAttendanceByDate(date)
        }
        return combine(studentsFlow, attendanceFlow) { students, attendance ->
            val attendanceByStudent = attendance.associateBy { it.studentId }
            students.map { student ->
                StudentAttendanceRecord(
                    student = student.toDomain(),
                    attendance = attendanceByStudent[student.id]?.toDomain()
                )
            }
        }
    }

    override suspend fun upsertAttendance(attendance: Attendance) {
        require(attendance.status != AttendanceStatus.UNMARKED) {
            "No se puede persistir asistencia sin marcar"
        }
        val courseId = attendance.courseId
        val subjectId = attendance.subjectId
        val existing = if (courseId != null && subjectId != null) {
            attendanceDao.getByStudentDateCourseSubject(
                studentId = attendance.studentId,
                date = attendance.date,
                courseId = courseId,
                subjectId = subjectId
            )
        } else {
            attendanceDao.getByStudentAndDate(attendance.studentId, attendance.date)
        }
        // markPending = true → arranca como SyncState.IDLE (a sincronizar).
        attendanceDao.insertOrReplaceAttendance(
            attendance.toEntity(existing = existing, markPending = true)
        )
    }
}
