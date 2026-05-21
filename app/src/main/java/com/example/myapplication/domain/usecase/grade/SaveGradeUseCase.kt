package com.example.myapplication.domain.usecase.grade

import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.repository.GradeRepository

class SaveGradeUseCase(
    private val repository: GradeRepository
) {
    suspend operator fun invoke(grade: Grade) {
        repository.upsertGrade(grade)
    }
}
