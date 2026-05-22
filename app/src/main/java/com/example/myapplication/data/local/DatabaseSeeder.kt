package com.example.myapplication.data.local

import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.data.local.entity.StudentEntity

/**
 * Población inicial de Room para desbloquear Asistencias y Calificaciones.
 * Solo inserta si las tablas están vacías (no re-seed en cada apertura).
 */
object DatabaseSeeder {

    private val demoStudents = listOf(
        StudentEntity(1, "María González Pérez", "5", "A", "Ana González", null),
        StudentEntity(2, "Carlos Rodríguez López", "5", "A", "Luis Rodríguez", null),
        StudentEntity(3, "Valentina Martínez Ruiz", "5", "A", "Carmen Martínez", null),
        StudentEntity(4, "Diego Hernández Silva", "5", "A", "Pedro Hernández", null),
        StudentEntity(5, "Sofía Pérez Morales", "5", "A", "Rosa Pérez", null),
        StudentEntity(6, "Andrés Jiménez Castro", "5", "A", "Jorge Jiménez", null),
        StudentEntity(7, "Isabella Torres Vega", "5", "A", "Patricia Torres", null),
        StudentEntity(8, "Mateo Ramírez Díaz", "5", "A", "Miguel Ramírez", null)
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
            grades += GradeEntity(
                studentId = student.id,
                description = "Evaluación diagnóstica",
                score = 16.0,
                maxScore = 20.0,
                subjectName = "General",
                periodName = "Actual",
                evaluationTypeName = "Diagnóstica"
            )
            grades += GradeEntity(
                studentId = student.id,
                description = "Trabajo en clase",
                score = 18.0,
                maxScore = 20.0,
                subjectName = "General",
                periodName = "Actual",
                evaluationTypeName = "Formativa"
            )
        }
        return grades
    }
}
