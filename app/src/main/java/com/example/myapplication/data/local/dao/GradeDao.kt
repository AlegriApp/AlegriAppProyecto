package com.example.myapplication.data.local.dao

import com.example.myapplication.data.local.entity.GradeEntity

interface GradeDao {
    suspend fun upsertAll(grades: List<GradeEntity>)
    suspend fun upsert(grade: GradeEntity)
    suspend fun getGradesByStudent(studentId: Long): List<GradeEntity>
    suspend fun getGradesBySubjectAndPeriod(subject: String, period: String): List<GradeEntity>
    suspend fun getPendingSync(): List<GradeEntity>
    suspend fun markAsSynced(ids: List<Long>)
}
