package com.example.myapplication.domain.usecase.grade

import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.repository.GradeRepository
import kotlinx.coroutines.flow.Flow

/**
 * Devuelve, de forma reactiva, todas las calificaciones registradas localmente
 * para un estudiante. Usado por la pantalla de detalle ([
 * com.example.myapplication.presentation.grades.GradeDetailViewModel]).
 *
 * La fuente es Room (offline-first): la UI siempre ve los datos locales y se
 * actualiza sola cuando la sincronización trae cambios desde Supabase.
 */
class GetGradesByStudentUseCase(
    private val repository: GradeRepository
) {
    operator fun invoke(studentId: Long): Flow<List<Grade>> =
        repository.observeGradesByStudent(studentId)
}
