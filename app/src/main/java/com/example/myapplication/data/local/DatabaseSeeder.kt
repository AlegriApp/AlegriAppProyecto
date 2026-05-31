package com.example.myapplication.data.local

import com.example.myapplication.core.common.newUuid
import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.data.local.entity.StudentEntity
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
    }

    private fun buildDemoGrades(): List<GradeEntity> {
        val grades = mutableListOf<GradeEntity>()
        demoStudents.forEach { student ->
            grades += demoGrade(student.id, "Evaluación diagnóstica", 16.0, "Diagnóstica")
            grades += demoGrade(student.id, "Trabajo en clase", 18.0, "Formativa")
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
        maxScore = 20.0,
        subjectName = "General",
        periodName = "Actual",
        evaluationTypeName = evaluationType,
        uuid = newUuid(),
        syncStatus = SyncState.Stored.SUCCESS
    )
}
