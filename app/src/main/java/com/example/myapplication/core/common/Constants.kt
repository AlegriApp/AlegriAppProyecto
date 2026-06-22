package com.example.myapplication.core.common

/**
 * Escala oficial de calificaciones del módulo de notas.
 *
 * Toda la app **maneja y muestra** las calificaciones sobre [MAX_SCORE].
 * Es la única fuente de verdad: si la institución cambia la escala o la nota
 * mínima de aprobación, se modifica AQUÍ y se propaga a UI, ViewModels y cálculos.
 */
object GradeScale {
    /** Nota máxima de la escala (sobre 10). */
    const val MAX_SCORE: Double = 10.0

    /** Versión entera de [MAX_SCORE] para mostrar en la UI (p.ej. "8 / 10"). */
    const val MAX_SCORE_INT: Int = 10

    /**
     * Nota mínima para aprobar. En Ecuador el mínimo es 7/10.
     * Por debajo de este valor el estudiante se considera "en riesgo".
     */
    const val PASSING_GRADE: Double = 7.0

    /** Normaliza una nota a la escala oficial [MAX_SCORE] sin importar su escala original. */
    fun normalize(score: Double, maxScore: Double): Double =
        if (maxScore > 0.0) score / maxScore * MAX_SCORE else score
}
