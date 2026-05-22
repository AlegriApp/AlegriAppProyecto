package com.example.myapplication.services.telegram

import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.AttendanceStatus
import com.example.myapplication.domain.model.Incident
import com.example.myapplication.domain.model.Student
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TelegramMessageBuilder {

    private const val MAX_MESSAGE_LENGTH = 4096

    fun buildAttendanceReport(
        date: String,
        courseName: String,
        records: List<Pair<Student, Attendance>>
    ): String {
        val present = records.count { it.second.status == AttendanceStatus.PRESENT }
        val late = records.count { it.second.status == AttendanceStatus.LATE }
        val absent = records.count { it.second.status == AttendanceStatus.ABSENT }
        val justified = records.count { it.second.status == AttendanceStatus.JUSTIFIED }
        val message = buildString {
            appendLine("AlegriAPP - Reporte de Asistencia")
            appendLine("Curso: ${sanitize(courseName)}")
            appendLine("Fecha: ${humanDate(date)}")
            appendLine("Presentes: $present")
            appendLine("Tardanzas: $late")
            appendLine("Ausentes: $absent")
            appendLine("Justificados: $justified")
            appendLine()
            appendLine("Detalle:")
            records.forEach { (student, attendance) ->
                appendLine("- ${sanitize(student.fullName)}: ${attendanceStatusLabel(attendance.status)}")
            }
        }
        return truncateMessage(message)
    }

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
            appendLine("Tipo: ${incident.type.label}")
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

    private fun attendanceStatusLabel(status: AttendanceStatus): String = when (status) {
        AttendanceStatus.PRESENT -> "Presente"
        AttendanceStatus.LATE -> "Atrasado"
        AttendanceStatus.ABSENT -> "Ausente"
        AttendanceStatus.JUSTIFIED -> "Justificado"
        AttendanceStatus.UNMARKED -> "Sin marcar"
    }
}
