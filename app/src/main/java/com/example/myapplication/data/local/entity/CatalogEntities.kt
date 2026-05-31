package com.example.myapplication.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_cursos")
data class CursoCatalogEntity(
    @PrimaryKey val id: Long,
    val nombre: String,
    val paralelo: String = "",
    @ColumnInfo(name = "anio_lectivo") val anioLectivo: String = "",
    @ColumnInfo(name = "periodo_academico_id") val periodoAcademicoId: Long? = null
)

@Entity(
    tableName = "catalog_materias",
    indices = [Index(value = ["curso_id"])]
)
data class MateriaCatalogEntity(
    @PrimaryKey val id: Long,
    val nombre: String,
    @ColumnInfo(name = "curso_id") val cursoId: Long
)

@Entity(tableName = "catalog_tipos_evaluacion")
data class TipoEvaluacionCatalogEntity(
    @PrimaryKey val id: Long,
    val nombre: String
)

@Entity(tableName = "catalog_periodos")
data class PeriodoAcademicoCatalogEntity(
    @PrimaryKey val id: Long,
    val nombre: String,
    @ColumnInfo(name = "anio_lectivo") val anioLectivo: String = ""
)

@Entity(tableName = "catalog_tipos_incidente")
data class TipoIncidenteCatalogEntity(
    @PrimaryKey val id: Long,
    val nombre: String
)

@Entity(
    tableName = "student_courses",
    indices = [
        Index(value = ["student_id"]),
        Index(value = ["course_id"]),
        Index(value = ["student_id", "course_id"], unique = true)
    ]
)
data class StudentCourseEntity(
    @PrimaryKey(autoGenerate = true) val pk: Long = 0L,
    @ColumnInfo(name = "student_id") val studentId: Long,
    @ColumnInfo(name = "course_id") val courseId: Long
)

@Entity(
    tableName = "student_representatives",
    indices = [
        Index(value = ["student_id"]),
        Index(value = ["representante_id"])
    ]
)
data class StudentRepresentativeEntity(
    @PrimaryKey(autoGenerate = true) val pk: Long = 0L,
    @ColumnInfo(name = "student_id") val studentId: Long,
    @ColumnInfo(name = "representante_id") val representanteId: Long,
    @ColumnInfo(name = "es_principal") val esPrincipal: Boolean = false
)

@Entity(
    tableName = "telegram_configs",
    indices = [Index(value = ["representante_id"])]
)
data class TelegramConfigEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "representante_id") val representanteId: Long?,
    @ColumnInfo(name = "chat_id") val chatId: String,
    @ColumnInfo(name = "bot_token") val botToken: String? = null,
    @ColumnInfo(name = "estado_integracion") val estadoIntegracion: String = "activo"
)
