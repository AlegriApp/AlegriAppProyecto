package com.example.myapplication.core.network

/**
 * Constantes alineadas con [alegriapp_create_tables_og.txt] (PostgreSQL / Supabase).
 * Los IDs por defecto deben existir en el proyecto remoto o ajustarse en local.properties.
 */
object SupabaseConfig {
    const val ESTUDIANTES_TABLE = "estudiantes"
    const val ASISTENCIAS_TABLE = "asistencias"
    const val CALIFICACIONES_TABLE = "calificaciones"

    const val ESTUDIANTE_SELECT =
        "id,codigo_institucional,nombre,apellido,estado," +
            "estudiante_curso(estado,cursos(paralelo,niveles_academicos(nombre)))"

    /** Estados válidos según CHECK en asistencias.estado */
    val ATTENDANCE_STATUSES = setOf("presente", "ausente", "atrasado", "justificado")

    /** Estados válidos según CHECK en calificaciones.estado */
    val GRADE_STATUSES = setOf("registrado", "revisado", "publicado", "anulado")
}
