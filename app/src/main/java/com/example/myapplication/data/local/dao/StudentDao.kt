package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceStudents(students: List<StudentEntity>)

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun getStudentById(studentId: Long): StudentEntity?

    @Query("SELECT COUNT(*) FROM students")
    suspend fun countStudents(): Int
}
