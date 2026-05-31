package com.example.myapplication.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v7 → v8: índice único de asistencias alineado con Supabase
 * UNIQUE (estudiante_id, curso_id, fecha, materia_id).
 */
val Migration_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_asistencias_estudiante_id_fecha")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asistencias_materia_id " +
                "ON asistencias (materia_id)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_asistencias_estudiante_id_curso_id_fecha_materia_id " +
                "ON asistencias (estudiante_id, curso_id, fecha, materia_id)"
        )
    }
}
