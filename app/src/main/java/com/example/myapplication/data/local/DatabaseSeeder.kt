package com.example.myapplication.data.local

import com.example.myapplication.core.common.newUuid
import com.example.myapplication.data.local.entity.CursoCatalogEntity
import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.data.local.entity.MateriaCatalogEntity
import com.example.myapplication.data.local.entity.PeriodoAcademicoCatalogEntity
import com.example.myapplication.data.local.entity.StudentEntity
import com.example.myapplication.data.local.entity.TipoEvaluacionCatalogEntity
import com.example.myapplication.data.local.entity.TipoIncidenteCatalogEntity
import com.example.myapplication.domain.model.sync.SyncState

/**
 * Población inicial de Room para desbloquear Asistencias y Calificaciones.
 * Solo inserta si las tablas están vacías (no re-seed en cada apertura).
 *
 * Los registros seed se marcan `SyncState.SUCCESS` y `localOnly`-equivalente:
 * son datos de demo, no deben sincronizarse a Supabase.
 */
object DatabaseSeeder {

    private val demoStudents = listOf(
        demoStudent(1, "María González Pérez"),
        demoStudent(2, "Carlos Rodríguez López"),
        demoStudent(3, "Valentina Martínez Ruiz"),
        demoStudent(4, "Diego Hernández Silva"),
        demoStudent(5, "Sofía Pérez Morales"),
        demoStudent(6, "Andrés Jiménez Castro"),
        demoStudent(7, "Isabella Torres Vega"),
        demoStudent(8, "Mateo Ramírez Díaz")
    )

    private fun demoStudent(id: Long, fullName: String) = StudentEntity(
        id = id,
        fullName = fullName,
        grade = "5",
        section = "A",
        representativeName = "Representante demo",
        telegramChatId = null,
        uuid = newUuid(),
        syncStatus = SyncState.Stored.SUCCESS
    )

    suspend fun seedIfEmpty(database: AppDatabase) {
        val studentDao = database.studentDao()
        if (studentDao.countStudents() == 0) {
            studentDao.insertOrReplaceStudents(demoStudents)
        }

        val gradeDao = database.gradeDao()
        if (gradeDao.countGrades() == 0) {
            gradeDao.insertOrReplaceGrades(buildDemoGrades())
        }

        seedCatalogsIfEmpty(database)
    }

    private suspend fun seedCatalogsIfEmpty(database: AppDatabase) {
        val catalogDao = database.catalogDao()
        if (catalogDao.countCursos() > 0) return

        val demoCourseId = 1L
        catalogDao.replaceCursos(
            listOf(
                CursoCatalogEntity(
                    id = demoCourseId,
                    nombre = "5to Grado",
                    paralelo = "A",
                    anioLectivo = "2025-2026"
                )
            )
        )
        catalogDao.replaceMaterias(
            listOf(
                MateriaCatalogEntity(id = 1L, nombre = "General", cursoId = demoCourseId)
            )
        )
        catalogDao.replaceTiposEvaluacion(
            listOf(
                TipoEvaluacionCatalogEntity(id = 6L, nombre = "Parcial"),
                TipoEvaluacionCatalogEntity(id = 7L, nombre = "Formativa")
            )
        )
        catalogDao.replacePeriodos(
            listOf(
                PeriodoAcademicoCatalogEntity(id = 1L, nombre = "Periodo actual", anioLectivo = "2025-2026")
            )
        )
        catalogDao.replaceTiposIncidente(
            listOf(
                TipoIncidenteCatalogEntity(id = 1L, nombre = "Conducta"),
                TipoIncidenteCatalogEntity(id = 2L, nombre = "Académico"),
                TipoIncidenteCatalogEntity(id = 4L, nombre = "Salud")
            )
        )
        val studentDao = database.studentDao()
        // Sin matrículas demo: al sincronizar se cargan desde estudiante_curso en Supabase.
    }

    private fun buildDemoGrades(): List<GradeEntity> {
        val grades = mutableListOf<GradeEntity>()
        // Notas demo sobre 10 (escala oficial GradeScale.MAX_SCORE).
        demoStudents.forEach { student ->
            grades += demoGrade(student.id, "Evaluación diagnóstica", 8.0, "Diagnóstica")
            grades += demoGrade(student.id, "Trabajo en clase", 9.0, "Formativa")
        }
        return grades
    }

    private fun demoGrade(
        studentId: Long,
        description: String,
        score: Double,
        evaluationType: String
    ) = GradeEntity(
        studentId = studentId,
        description = description,
        score = score,
        maxScore = com.example.myapplication.core.common.GradeScale.MAX_SCORE,
        subjectName = "General",
        periodName = "Actual",
        evaluationTypeName = evaluationType,
        uuid = newUuid(),
        syncStatus = SyncState.Stored.SUCCESS
    )
}
