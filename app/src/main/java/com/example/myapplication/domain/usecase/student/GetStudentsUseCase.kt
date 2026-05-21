package com.example.myapplication.domain.usecase.student

import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow

class GetStudentsUseCase(
    private val repository: StudentRepository
) {
    operator fun invoke(): Flow<List<Student>> = repository.observeStudents()
}
