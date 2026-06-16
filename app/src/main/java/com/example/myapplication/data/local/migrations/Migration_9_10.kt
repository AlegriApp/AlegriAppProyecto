package com.example.myapplication.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v9 -> v10: cache local de cursos asignados a cada docente.
 */
val Migration_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS teacher_courses (" +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "teacher_id INTEGER NOT NULL, " +
                "course_id INTEGER NOT NULL, " +
                "materia_id INTEGER, " +
                "es_tutor INTEGER NOT NULL DEFAULT 0" +
                ")"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_teacher_courses_teacher_id ON teacher_courses (teacher_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_teacher_courses_course_id ON teacher_courses (course_id)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_teacher_courses_teacher_id_course_id " +
                "ON teacher_courses (teacher_id, course_id)"
        )
    }
}
