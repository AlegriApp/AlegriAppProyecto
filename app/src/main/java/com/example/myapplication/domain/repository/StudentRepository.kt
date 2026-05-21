package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    fun observeStudents(): Flow<List<Student>>
}
