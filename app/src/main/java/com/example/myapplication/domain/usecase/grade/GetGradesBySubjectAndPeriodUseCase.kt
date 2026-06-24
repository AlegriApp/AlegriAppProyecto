package com.example.myapplication.domain.usecase.grade

import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.repository.GradeRepository
import kotlinx.coroutines.flow.Flow

class GetGradesBySubjectAndPeriodUseCase(
    private val repository: GradeRepository
) {
    operator fun invoke(subject: String, period: String): Flow<List<Grade>> =
        repository.observeGradesBySubjectAndPeriod(subject, period)
}
