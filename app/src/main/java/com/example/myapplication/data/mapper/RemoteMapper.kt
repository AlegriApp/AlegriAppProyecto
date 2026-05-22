package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entity.AttendanceEntity
import com.example.myapplication.data.local.entity.GradeEntity
import com.example.myapplication.data.local.entity.StudentEntity
import com.example.myapplication.data.remote.dto.AsistenciaInsertDto
import com.example.myapplication.data.remote.dto.CalificacionInsertDto
import com.example.myapplication.data.remote.dto.EstudianteRemoteDto
import com.example.myapplication.domain.model.AttendanceStatus

fun EstudianteRemoteDto.toStudentEntity(): StudentEntity {
    val activeEnrollment = estudianteCurso
        ?.firstOrNull { it.estado == "activo" }
        ?: estudianteCurso?.firstOrNull()

    val nivel = activeEnrollment?.cursos?.nivelAcademico?.nombre?.trim().orEmpty()
    val paralelo = activeEnrollment?.cursos?.paralelo?.trim().orEmpty()

    return StudentEntity(
        id = id,
        fullName = listOf(nombre, apellido).joinToString(" ").trim(),
        grade = nivel.ifBlank { "Sin nivel" },
        section = paralelo.ifBlank { "?" },
        representativeName = "",
        telegramChatId = null
    )
}

fun AttendanceEntity.toAsistenciaInsertDto(defaultCourseId: Long): AsistenciaInsertDto = AsistenciaInsertDto(
    estudianteId = studentId,
    cursoId = courseId ?: defaultCourseId,
    materiaId = subjectId,
    fecha = date,
    horaEntrada = entryTime,
    estado = status.toRemoteAttendanceStatus(),
    observacion = observation,
    justificacion = justification,
    docenteId = teacherId,
    sincronizacionPendiente = false
)

fun GradeEntity.toCalificacionInsertDto(
    defaultCourseId: Long,
    defaultMateriaId: Long,
    defaultTipoEvaluacionId: Long,
    defaultPeriodoId: Long?
): CalificacionInsertDto = CalificacionInsertDto(
    estudianteId = studentId,
    materiaId = subjectId ?: defaultMateriaId,
    cursoId = courseId ?: defaultCourseId,
    periodoAcademicoId = periodAcademicId ?: defaultPeriodoId,
    tipoEvaluacionId = evaluationTypeId ?: defaultTipoEvaluacionId,
    descripcion = description,
    notaObtenida = score,
    notaMaxima = maxScore,
    observacion = observation,
    docenteId = teacherId,
    estado = state
)

private fun String.toRemoteAttendanceStatus(): String = when (this.lowercase()) {
    "presente", "present" -> "presente"
    "atrasado", "late" -> "atrasado"
    "ausente", "absent" -> "ausente"
    "justificado", "justified" -> "justificado"
    else -> "presente"
}

fun AttendanceStatus.toRemoteStatus(): String = when (this) {
    AttendanceStatus.PRESENT -> "presente"
    AttendanceStatus.LATE -> "atrasado"
    AttendanceStatus.ABSENT -> "ausente"
    AttendanceStatus.JUSTIFIED -> "justificado"
    AttendanceStatus.UNMARKED -> "ausente"
}
