package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.dao.AttendanceDao
import com.example.myapplication.data.local.dao.CatalogDao
import com.example.myapplication.data.local.dao.GradeDao
import com.example.myapplication.data.local.dao.IncidentDao
import com.example.myapplication.data.local.dao.StudentDao
import com.example.myapplication.data.local.entity.AttendanceEntity
import com.example.myapplication.data.local.entity.CursoCatalogEntity
import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.data.local.entity.IncidentEntity
import com.example.myapplication.data.local.entity.MateriaCatalogEntity
import com.example.myapplication.data.local.entity.PeriodoAcademicoCatalogEntity
import com.example.myapplication.data.local.entity.StudentCourseEntity
import com.example.myapplication.data.local.entity.StudentEntity
import com.example.myapplication.data.local.entity.StudentRepresentativeEntity
import com.example.myapplication.data.local.entity.TelegramConfigEntity
import com.example.myapplication.data.local.entity.TipoEvaluacionCatalogEntity
import com.example.myapplication.data.local.entity.TipoIncidenteCatalogEntity

@Database(
    entities = [
        StudentEntity::class,
        AttendanceEntity::class,
        GradeEntity::class,
        IncidentEntity::class,
        CursoCatalogEntity::class,
        MateriaCatalogEntity::class,
        TipoEvaluacionCatalogEntity::class,
        PeriodoAcademicoCatalogEntity::class,
        TipoIncidenteCatalogEntity::class,
        StudentCourseEntity::class,
        StudentRepresentativeEntity::class,
        TelegramConfigEntity::class
    ],
    version = 9,
    exportSchema = false
)
/**
 * Room principal. La población demo se ejecuta en [com.example.myapplication.data.local.DatabaseSeeder]
 * al crear la instancia vía [com.example.myapplication.core.di.AppModule].
 *
 * **v6 (Offline First):** añadidas columnas `uuid`, `remote_id`, `sync_status`,
 * `sync_error`, `last_sync_attempt`, `server_updated_at`, `is_deleted` a todas
 * las tablas mobile. La columna `sincronizacion_pendiente` se conserva durante
 * la transición para no romper consultas viejas. Ver Migration_5_6.
 */
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun gradeDao(): GradeDao
    abstract fun incidentDao(): IncidentDao
    abstract fun catalogDao(): CatalogDao
}
