package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface StudentRepository {
    fun observeStudents(): Flow<List<Student>>
    suspend fun upsertStudents(students: List<Student>)
    /** Busca un estudiante local por su PK Room. Devuelve null si no existe o fue soft-deleted. */
    suspend fun findById(studentId: Long): Student?
}
