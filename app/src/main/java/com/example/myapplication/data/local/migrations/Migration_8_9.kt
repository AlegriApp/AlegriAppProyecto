package com.example.myapplication.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v8 → v9: repara instalaciones que aplicaron Migration_7_8 sin
 * `index_asistencias_materia_id` (Room validaba el esquema y crasheaba).
 */
val Migration_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_asistencias_materia_id " +
                "ON asistencias (materia_id)"
        )
    }
}
