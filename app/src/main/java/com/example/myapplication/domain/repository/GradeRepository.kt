package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.Grade
import kotlinx.coroutines.flow.Flow

interface GradeRepository {
    fun observeGradesBySubjectAndPeriod(subject: String, period: String): Flow<List<Grade>>
    fun observeGradesByCatalogFilters(
        courseId: Long,
        subjectId: Long,
        evaluationTypeId: Long,
        periodId: Long
    ): Flow<List<Grade>>
    suspend fun upsertGrade(grade: Grade)
}
