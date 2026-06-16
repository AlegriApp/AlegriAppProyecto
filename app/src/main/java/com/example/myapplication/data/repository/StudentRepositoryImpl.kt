package com.example.myapplication.data.repository

import com.example.myapplication.core.preferences.AuthPreferences
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.mapper.toDomainList
import com.example.myapplication.data.mapper.toEntity
import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.repository.StudentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(
    private val studentDao: StudentDao,
    private val authPreferences: AuthPreferences
) : StudentRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeStudents(): Flow<List<Student>> =
        authPreferences.session.flatMapLatest { session ->
            if (session == null) {
                studentDao.observeStudents()
            } else {
                studentDao.observeStudentsForTeacher(session.userId)
            }
        }.map { entities -> entities.toDomainList() }

    override fun observeStudentsByCourse(courseId: Long): Flow<List<Student>> =
        studentDao.observeStudentsByCourse(courseId).map { it.toDomainList() }

    override suspend fun upsertStudents(students: List<Student>) {
        if (students.isEmpty()) return
        val entities = students.map { student ->
            val existing = studentDao.getStudentById(student.id)
            student.toEntity(existing = existing)
        }
        studentDao.insertOrReplaceStudents(entities)
    }

    override suspend fun findById(studentId: Long): Student? =
        studentDao.getStudentById(studentId)?.toDomain()
}
