package com.example.myapplication.domain.usecase.grade

import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.repository.GradeRepository
import kotlinx.coroutines.flow.Flow

class GetGradesByCatalogFiltersUseCase(
    private val repository: GradeRepository
) {
    operator fun invoke(
        courseId: Long,
        subjectId: Long,
        evaluationTypeId: Long,
        periodId: Long
    ): Flow<List<Grade>> = repository.observeGradesByCatalogFilters(
        courseId = courseId,
        subjectId = subjectId,
        evaluationTypeId = evaluationTypeId,
        periodId = periodId
    )
}
