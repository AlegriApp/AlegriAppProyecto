package com.example.myapplication.domain.usecase.student

import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observa de forma reactiva un estudiante por su id (PK local de Room).
 * Emite null si el estudiante no existe o fue eliminado lógicamente.
 */
class GetStudentByIdUseCase(
    private val repository: StudentRepository
) {
    operator fun invoke(studentId: Long): Flow<Student?> =
        repository.observeStudentById(studentId)
}
