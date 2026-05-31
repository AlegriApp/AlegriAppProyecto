package com.example.myapplication.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migración Room v5 → v6: añade infraestructura Offline First.
 *
 * Por cada tabla mobile (`asistencias`, `calificaciones`, `incidentes`, `students`):
 *   - Columna `uuid TEXT NOT NULL DEFAULT ''` (luego se rellena con UUID v4)
 *   - Columna `remote_id INTEGER` (SERIAL de Postgres tras primer sync)
 *   - Columna `sync_status TEXT NOT NULL DEFAULT 'IDLE'`
 *   - Columna `sync_error TEXT`
 *   - Columna `last_sync_attempt INTEGER`
 *   - Columna `server_updated_at INTEGER`
 *   - Columna `is_deleted INTEGER NOT NULL DEFAULT 0`
 *   - Índice único en `uuid`
 *   - Índices en `sync_status` y `is_deleted`
 *
 * `incidentes` añade además `local_only INTEGER NOT NULL DEFAULT 1` para distinguir
 * registros locales (que NUNCA viajan a Supabase) vs los que vienen del PULL.
 *
 * Backfill (preserva datos pendientes existentes):
 *   - `sincronizacion_pendiente = 1` → `sync_status = 'IDLE'`
 *   - `sincronizacion_pendiente = 0` → `sync_status = 'SUCCESS'`
 *   - `uuid` vacío → UUID v4 generado en SQLite con `randomblob`
 *
 * La columna `sincronizacion_pendiente` se conserva durante la transición
 * (no se elimina) para no romper código viejo que aún la consulta.
 */
val Migration_5_6 = object : Migration(5, 6) {

    private val syncTables = listOf(
        "asistencias",
        "calificaciones",
        "incidentes",
        "students"
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        syncTables.forEach { table ->
            addOfflineFirstColumns(db, table)
        }

        // `incidentes` necesita además `local_only` para PULL-only correctness.
        db.execSQL("ALTER TABLE incidentes ADD COLUMN local_only INTEGER NOT NULL DEFAULT 1")

        syncTables.forEach { table ->
            backfillSyncStatus(db, table)
            backfillUuids(db, table)
            createOfflineFirstIndexes(db, table)
        }
    }

    private fun addOfflineFirstColumns(db: SupportSQLiteDatabase, table: String) {
        db.execSQL("ALTER TABLE $table ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE $table ADD COLUMN remote_id INTEGER")
        db.execSQL("ALTER TABLE $table ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'IDLE'")
        db.execSQL("ALTER TABLE $table ADD COLUMN sync_error TEXT")
        db.execSQL("ALTER TABLE $table ADD COLUMN last_sync_attempt INTEGER")
        db.execSQL("ALTER TABLE $table ADD COLUMN server_updated_at INTEGER")
        db.execSQL("ALTER TABLE $table ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0")
    }

    private fun backfillSyncStatus(db: SupportSQLiteDatabase, table: String) {
        if (!tableHasColumn(db, table, "sincronizacion_pendiente")) return
        db.execSQL(
            """
            UPDATE $table
            SET sync_status = CASE
                WHEN sincronizacion_pendiente = 1 THEN 'IDLE'
                ELSE 'SUCCESS'
            END
            WHERE sync_status = 'IDLE'
            """.trimIndent()
        )
    }

    /**
     * Rellena UUIDs vacíos con uuid v4 generado vía randomblob (SQLite).
     * Formato: xxxxxxxx-xxxx-4xxx-Yxxx-xxxxxxxxxxxx (Y ∈ 8,9,a,b).
     */
    private fun backfillUuids(db: SupportSQLiteDatabase, table: String) {
        db.execSQL(
            """
            UPDATE $table
            SET uuid =
                lower(hex(randomblob(4))) || '-' ||
                lower(hex(randomblob(2))) || '-4' ||
                substr(lower(hex(randomblob(2))), 2) || '-' ||
                substr('89ab', abs(random()) % 4 + 1, 1) ||
                substr(lower(hex(randomblob(2))), 2) || '-' ||
                lower(hex(randomblob(6)))
            WHERE uuid = '' OR uuid IS NULL
            """.trimIndent()
        )
    }

    private fun createOfflineFirstIndexes(db: SupportSQLiteDatabase, table: String) {
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_uuid ON $table(uuid)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_${table}_sync_status ON $table(sync_status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_${table}_is_deleted ON $table(is_deleted)")
    }

    private fun tableHasColumn(
        db: SupportSQLiteDatabase,
        table: String,
        column: String
    ): Boolean {
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIdx = cursor.getColumnIndex("name")
            if (nameIdx < 0) return false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIdx).equals(column, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
