package com.example.myapplication.core.network

/**
 * Constantes alineadas con [alegriapp_create_tables_og.txt] (PostgreSQL / Supabase).
 * Los IDs por defecto deben existir en el proyecto remoto o ajustarse en local.properties.
 */
object SupabaseConfig {
    const val ESTUDIANTES_TABLE = "estudiantes"
    const val ASISTENCIAS_TABLE = "asistencias"
    const val CALIFICACIONES_TABLE = "calificaciones"

    /** PULL only — mobile no inserta, solo lee. */
    const val INCIDENTES_TABLE = "incidentes"
    const val TIPOS_INCIDENTE_TABLE = "tipos_incidente"

    const val ESTUDIANTE_SELECT =
        "id,uuid,codigo_institucional,nombre,apellido,estado,updated_at,deleted_at," +
            "estudiante_curso(estado,cursos(paralelo,niveles_academicos(nombre)))"

    /**
     * Select de incidentes para PULL. Trae solo columnas necesarias para mobile.
     * No incluye `created_by`/`updated_by` para evitar fugas de IDs de usuario.
     */
    const val INCIDENTE_SELECT =
        "id,uuid,estudiante_id,tipo_incidente_id,descripcion,fecha_hora," +
            "nivel_gravedad,estado,observaciones,reportado_por_id," +
            "updated_at,deleted_at"

    /** Estados válidos según CHECK en asistencias.estado */
    val ATTENDANCE_STATUSES = setOf("presente", "ausente", "atrasado", "justificado")

    /** Estados válidos según CHECK en calificaciones.estado */
    val GRADE_STATUSES = setOf("registrado", "revisado", "publicado", "anulado")

    /** Estados válidos según CHECK en incidentes.estado */
    val INCIDENT_STATES = setOf("abierto", "en_seguimiento", "cerrado", "archivado")

    /** Niveles válidos según CHECK en incidentes.nivel_gravedad */
    val INCIDENT_SEVERITIES = setOf("bajo", "medio", "alto", "critico")
}
