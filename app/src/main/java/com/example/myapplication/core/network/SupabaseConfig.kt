package com.example.myapplication.core.network

/**
 * Constantes alineadas con [alegriapp_create_tables_og.txt] (PostgreSQL / Supabase).
 * Los IDs por defecto deben existir en el proyecto remoto o ajustarse en local.properties.
 */
object SupabaseConfig {
    const val USUARIOS_TABLE = "usuarios"
    const val ESTUDIANTES_TABLE = "estudiantes"
    const val ASISTENCIAS_TABLE = "asistencias"
    const val CALIFICACIONES_TABLE = "calificaciones"

    /** PULL only — mobile no inserta, solo lee. */
    const val INCIDENTES_TABLE = "incidentes"
    const val TIPOS_INCIDENTE_TABLE = "tipos_incidente"
    const val CURSOS_TABLE = "cursos"
    const val MATERIAS_TABLE = "materias"
    const val TIPOS_EVALUACION_TABLE = "tipos_evaluacion"
    const val PERIODOS_ACADEMICOS_TABLE = "periodos_academicos"
    const val CONFIGURACION_TELEGRAM_TABLE = "configuracion_telegram"
    const val ESTUDIANTE_CURSO_TABLE = "estudiante_curso"
    const val DOCENTE_CURSO_TABLE = "docente_curso"

    /**
     * Select alineado con [alegriapp_create_tables_og.txt]: `estudiante_representante` no tiene
     * columna `estado` (solo `deleted_at`). Pedir `estado` en el embed provoca HTTP 400 en PostgREST.
     */
    const val ESTUDIANTE_SELECT =
        "id,uuid,codigo_institucional,nombre,apellido,estado,updated_at,deleted_at," +
            "estudiante_curso(curso_id,estado,cursos(id,nombre,paralelo,niveles_academicos(nombre)))," +
            "estudiante_representante(es_principal,representante_id,deleted_at," +
            "representantes(id,nombre,apellido,configuracion_telegram(id,chat_id,token_bot_encriptado,estado_integracion,deleted_at)))"

    const val USUARIO_SELECT =
        "id,nombre,apellido,email,password_hash,rol_id,estado,ultimo_acceso,deleted_at,roles(nombre)"

    const val DOCENTE_CURSO_SELECT =
        "docente_id,curso_id,materia_id,es_tutor,estado,deleted_at," +
            "cursos(id,nombre,paralelo,anio_lectivo,periodo_academico_id,estado)"

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
