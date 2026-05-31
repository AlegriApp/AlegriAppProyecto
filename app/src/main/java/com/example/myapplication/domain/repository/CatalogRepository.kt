package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.AcademicPeriodCatalog
import com.example.myapplication.domain.model.CourseCatalog
import com.example.myapplication.domain.model.EvaluationTypeCatalog
import com.example.myapplication.domain.model.IncidentTypeCatalog
import com.example.myapplication.domain.model.SubjectCatalog
import com.example.myapplication.domain.model.TelegramDestination
import com.example.myapplication.domain.model.sync.SyncOutcome
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun observeCourses(): Flow<List<CourseCatalog>>
    fun observeSubjectsByCourse(courseId: Long): Flow<List<SubjectCatalog>>
    fun observeEvaluationTypes(): Flow<List<EvaluationTypeCatalog>>
    fun observeAcademicPeriods(): Flow<List<AcademicPeriodCatalog>>
    fun observeIncidentTypes(): Flow<List<IncidentTypeCatalog>>
    suspend fun resolveTelegramForStudent(studentId: Long): TelegramDestination?
    suspend fun syncCatalogsFromRemote(): SyncOutcome
}
