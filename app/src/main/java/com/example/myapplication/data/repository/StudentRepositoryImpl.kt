package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.mapper.toDomainList
import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(
    private val studentDao: StudentDao
) : StudentRepository {
    override fun observeStudents(): Flow<List<Student>> = studentDao.observeStudents().map { entities ->
        entities.toDomainList()
    }
}
