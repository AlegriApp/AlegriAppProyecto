package com.example.myapplication.services.telegram

import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.AttendanceStatus
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.IncidentType
import com.example.myapplication.domain.model.Student
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TelegramMessageBuilder {

    private const val MAX_MESSAGE_LENGTH = 4096

    fun buildAttendanceReport(
        date: String,
        courseName: String,
        records: List<Pair<Student, Attendance>>,
        subjectName: String = ""
    ): String {
        val present = records.count { it.second.status == AttendanceStatus.PRESENT }
        val absent = records.count { it.second.status == AttendanceStatus.ABSENT }
        val message = buildString {
            appendLine("AlegriAPP - Reporte de Asistencia")
            appendLine("Curso: ${sanitize(courseName)}")
            if (subjectName.isNotBlank()) {
                appendLine("Materia: ${sanitize(subjectName)}")
            }
            appendLine("Fecha: ${humanDate(date)}")
            appendLine("Presentes: $present")
            appendLine("Ausentes: $absent")
            appendLine()
            appendLine("Detalle:")
            records.forEach { (student, attendance) ->
                appendLine("- ${sanitize(student.fullName)}: ${attendanceStatusLabel(attendance.status)}")
            }
        }
        return truncateMessage(message)
    }

    /** Mensaje individual de asistencia para el representante de un estudiante. */
    fun buildAttendanceReportForParent(
        student: Student,
        status: AttendanceStatus,
        date: String,
        courseName: String,
        subjectName: String = ""
    ): String = buildString {
        appendLine("AlegriAPP - Asistencia de su representado/a")
        appendLine("Estudiante: ${sanitize(student.fullName)}")
        appendLine("Curso: ${sanitize(courseName)}")
        if (subjectName.isNotBlank()) {
            appendLine("Materia: ${sanitize(subjectName)}")
        }
        appendLine("Fecha: ${humanDate(date)}")
        appendLine("Estado: ${attendanceStatusLabel(status)}")
        if (student.representativeName.isNotBlank()) {
            appendLine("Representante: ${sanitize(student.representativeName)}")
        }
    }.let(::truncateMessage)

    /** Boletín de calificaciones para el representante de un estudiante. */
    fun buildGradeReportForParent(
        student: Student,
        subject: String,
        period: String,
        courseName: String,
        lines: List<GradeActivityLine>
    ): String {
        val average = lines.map { it.score }.average().takeIf { !it.isNaN() } ?: 0.0
        val maxScore = lines.maxOfOrNull { it.maxScore } ?: 20.0
        return buildString {
            appendLine("AlegriAPP - Calificaciones de su representado/a")
            appendLine("Estudiante: ${sanitize(student.fullName)}")
            if (courseName.isNotBlank()) {
                appendLine("Curso: ${sanitize(courseName)}")
            }
            appendLine("Materia: ${sanitize(subject)}")
            appendLine("Periodo: ${sanitize(period)}")
            appendLine("Promedio: ${"%.2f".format(average)} / ${"%.2f".format(maxScore)}")
            appendLine()
            appendLine("Detalle:")
            lines.forEach { line ->
                appendLine(
                    "- ${sanitize(line.activityName)} (${sanitize(line.activityType)}): " +
                        "${"%.2f".format(line.score)}/${"%.2f".format(line.maxScore)}"
                )
            }
        }.let(::truncateMessage)
    }

    data class GradeActivityLine(
        val activityName: String,
        val activityType: String,
        val score: Double,
        val maxScore: Double
    )

    fun buildGradeReport(
        subject: String,
        period: String,
        records: List<GradeReportRecord>
    ): String {
        val average = records.map { it.averageScore }.average().takeIf { !it.isNaN() } ?: 0.0
        val message = buildString {
            appendLine("AlegriAPP - Boletin Academico")
            appendLine("Materia: ${sanitize(subject)}")
            appendLine("Periodo: ${sanitize(period)}")
            appendLine("Promedio: ${"%.2f".format(average)}")
            appendLine()
            appendLine("Detalle:")
            records.forEach { record ->
                appendLine(
                    "- ${sanitize(record.student.fullName)}: " +
                        "${"%.2f".format(record.averageScore)}/${"%.2f".format(record.maxScore)}"
                )
            }
        }
        return truncateMessage(message)
    }

    data class GradeReportRecord(
        val student: Student,
        val averageScore: Double,
        val maxScore: Double
    )

    fun buildIncidentReport(
        student: Student,
        incident: Incident
    ): String {
        val message = buildString {
            appendLine("AlegriAPP - Reporte de Incidente")
            appendLine("Estudiante: ${sanitize(student.fullName)}")
            appendLine("Curso: ${sanitize(student.grade)} ${sanitize(student.section)}".trim())
            appendLine("Tipo: ${formatIncidentType(incident.type)}")
            appendLine("Severidad: ${incident.severity.label}")
            appendLine("Fecha y hora: ${humanDateTime(incident.dateTime)}")
            if (!incident.teacherName.isNullOrBlank()) {
                appendLine("Docente: ${sanitize(incident.teacherName)}")
            }
            appendLine()
            appendLine("Descripcion:")
            appendLine(sanitize(incident.description))
        }
        return truncateMessage(message)
    }

    private fun truncateMessage(message: String): String {
        if (message.length <= MAX_MESSAGE_LENGTH) return message
        val suffix = "\n...(mensaje truncado)"
        return message.take(MAX_MESSAGE_LENGTH - suffix.length) + suffix
    }

    private fun sanitize(value: String): String = value.trim()

    private fun humanDate(date: String): String {
        val parsed = runCatching { LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
        return parsed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: date
    }

    private fun humanDateTime(dateTime: String): String {
        val parsed = runCatching { LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }.getOrNull()
        return parsed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) ?: dateTime
    }

    private fun formatIncidentType(type: String): String =
        runCatching { IncidentType.valueOf(type).label }.getOrElse {
            type.toLongOrNull()?.let { "Incidente (catálogo #$it)" } ?: type
        }

    private fun attendanceStatusLabel(status: AttendanceStatus): String = when (status) {
        AttendanceStatus.PRESENT -> "Presente"
        AttendanceStatus.ABSENT -> "Ausente"
        AttendanceStatus.UNMARKED -> "Sin marcar"
    }
}
