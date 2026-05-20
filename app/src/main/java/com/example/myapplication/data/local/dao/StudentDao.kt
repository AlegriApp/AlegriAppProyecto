package com.example.myapplication.data.local.dao

import com.example.myapplication.data.local.entity.StudentEntity

interface StudentDao {
    suspend fun upsertAll(students: List<StudentEntity>)
    suspend fun upsert(student: StudentEntity)
    suspend fun getStudentsBySection(gradeSection: String): List<StudentEntity>
    suspend fun getAllStudents(): List<StudentEntity>
}
