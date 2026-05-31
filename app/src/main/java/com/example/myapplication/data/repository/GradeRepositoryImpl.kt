package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.GradeDao
import com.example.myapplication.data.mapper.toDomain
import com.example.myapplication.data.mapper.toEntity
import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.repository.GradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GradeRepositoryImpl(
    private val gradeDao: GradeDao
) : GradeRepository {
    override fun observeGradesBySubjectAndPeriod(subject: String, period: String): Flow<List<Grade>> =
        gradeDao.observeGradesBySubjectAndPeriod(subject, period).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun upsertGrade(grade: Grade) {
        val existing = gradeDao.getByStudentSubjectPeriodAndDescription(
            studentId = grade.studentId,
            subject = grade.subject,
            period = grade.period,
            description = grade.activityName
        )
        gradeDao.insertOrReplaceGrade(grade.toEntity(existing = existing, markPending = true))
    }
}
