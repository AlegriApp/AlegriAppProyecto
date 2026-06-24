package com.example.myapplication.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Catálogos (cursos, materias, tipos evaluación, periodos, tipos incidente),
 * relación estudiante–curso, representantes y configuración Telegram.
 */
object Migration_6_7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS catalog_cursos (
                id INTEGER NOT NULL PRIMARY KEY,
                nombre TEXT NOT NULL,
                paralelo TEXT NOT NULL DEFAULT '',
                anio_lectivo TEXT NOT NULL DEFAULT '',
                periodo_academico_id INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS catalog_materias (
                id INTEGER NOT NULL PRIMARY KEY,
                nombre TEXT NOT NULL,
                curso_id INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_catalog_materias_curso_id ON catalog_materias(curso_id)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS catalog_tipos_evaluacion (
                id INTEGER NOT NULL PRIMARY KEY,
                nombre TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS catalog_periodos (
                id INTEGER NOT NULL PRIMARY KEY,
                nombre TEXT NOT NULL,
                anio_lectivo TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS catalog_tipos_incidente (
                id INTEGER NOT NULL PRIMARY KEY,
                nombre TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS student_courses (
                pk INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                student_id INTEGER NOT NULL,
                course_id INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_student_courses_student_id ON student_courses(student_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_student_courses_course_id ON student_courses(course_id)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_student_courses_student_id_course_id " +
                "ON student_courses(student_id, course_id)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS student_representatives (
                pk INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                student_id INTEGER NOT NULL,
                representante_id INTEGER NOT NULL,
                es_principal INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_student_representatives_student_id " +
                "ON student_representatives(student_id)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS telegram_configs (
                id INTEGER NOT NULL PRIMARY KEY,
                representante_id INTEGER,
                chat_id TEXT NOT NULL,
                bot_token TEXT,
                estado_integracion TEXT NOT NULL DEFAULT 'activo'
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_telegram_configs_representante_id " +
                "ON telegram_configs(representante_id)"
        )
        if (!db.hasColumn("students", "representative_id")) {
            db.execSQL("ALTER TABLE students ADD COLUMN representative_id INTEGER")
        }
    }

    private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
        query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }
}
