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
    override fun observeGradesByStudent(studentId: Long): Flow<List<Grade>> =
        gradeDao.observeGradesByStudent(studentId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeGradesBySubjectAndPeriod(subject: String, period: String): Flow<List<Grade>> =
        gradeDao.observeGradesBySubjectAndPeriod(subject, period).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeGradesByCatalogFilters(
        courseId: Long,
        subjectId: Long,
        evaluationTypeId: Long,
        periodId: Long
    ): Flow<List<Grade>> =
        gradeDao.observeGradesByCatalogFilters(courseId, subjectId, evaluationTypeId, periodId)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertGrade(grade: Grade) {
        val existing = if (
            grade.courseId != null &&
            grade.subjectId != null &&
            grade.periodAcademicId != null &&
            grade.evaluationTypeId != null
        ) {
            gradeDao.getByStudentCatalogAndDescription(
                studentId = grade.studentId,
                subjectId = grade.subjectId,
                periodId = grade.periodAcademicId,
                evaluationTypeId = grade.evaluationTypeId,
                description = grade.activityName
            )
        } else {
            gradeDao.getByStudentSubjectPeriodAndDescription(
                studentId = grade.studentId,
                subject = grade.subject,
                period = grade.period,
                description = grade.activityName
            )
        }
        gradeDao.insertOrReplaceGrade(grade.toEntity(existing = existing, markPending = true))
    }
}
