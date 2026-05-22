package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.dao.AttendanceDao
import com.example.myapplication.data.local.dao.GradeDao
import com.example.myapplication.data.local.dao.IncidentDao
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.local.entity.AttendanceEntity
import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.data.local.entity.IncidentEntity
import com.example.myapplication.data.local.entity.StudentEntity

@Database(
    entities = [
        StudentEntity::class,
        AttendanceEntity::class,
        GradeEntity::class,
        IncidentEntity::class
    ],
    version = 5,
    exportSchema = false
)
/**
 * Room principal. La población demo se ejecuta en [com.example.myapplication.data.local.DatabaseSeeder]
 * al crear la instancia vía [com.example.myapplication.core.di.AppModule].
 */
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun gradeDao(): GradeDao
    abstract fun incidentDao(): IncidentDao
}
