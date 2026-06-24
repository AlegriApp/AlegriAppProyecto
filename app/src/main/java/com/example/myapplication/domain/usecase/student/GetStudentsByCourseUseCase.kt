package com.example.myapplication.domain.usecase.student

import com.example.myapplication.domain.model.Student
import com.example.myapplication.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow

class GetStudentsByCourseUseCase(
    private val repository: StudentRepository
) {
    operator fun invoke(courseId: Long): Flow<List<Student>> =
        repository.observeStudentsByCourse(courseId)
}
