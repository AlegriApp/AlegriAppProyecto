package com.example.myapplication.services.telegram

import com.example.myapplication.domain.model.Attendance
import com.example.myapplication.domain.model.AttendanceStatus
import com.example.myapplication.domain.model.Grade
import com.example.myapplication.domain.model.Student
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TelegramMessageBuilder {

    fun buildAttendanceReport(
        date: String,
        courseName: String,
        records: List<Pair<Student, Attendance>>
    ): String {
        val present = records.count { it.second.status == AttendanceStatus.PRESENT }
        val late = records.count { it.second.status == AttendanceStatus.LATE }
        val absent = records.count {
            it.second.status == AttendanceStatus.ABSENT || it.second.status == AttendanceStatus.JUSTIFIED
        }
        return buildString {
            appendLine("*AlegriAPP - Reporte de Asistencia*")
            appendLine("Curso: $courseName")
            appendLine("Fecha: ${humanDate(date)}")
            appendLine("Presentes: $present")
            appendLine("Tardanzas: $late")
            appendLine("Ausentes: $absent")
            appendLine()
            appendLine("*Detalle:*")
            records.forEach { (student, attendance) ->
                appendLine("- ${student.fullName}: ${attendanceStatusLabel(attendance.status)}")
            }
        }
    }

    fun buildGradeReport(
        subject: String,
        period: String,
        records: List<Pair<Student, Grade>>
    ): String {
        val average = records.map { it.second.score }.average().takeIf { !it.isNaN() } ?: 0.0
        return buildString {
            appendLine("*AlegriAPP - Boletin Academico*")
            appendLine("Materia: $subject")
            appendLine("Periodo: $period")
            appendLine("Promedio: ${"%.2f".format(average)}")
            appendLine()
            appendLine("*Detalle:*")
            records.forEach { (student, grade) ->
                appendLine("- ${student.fullName}: ${"%.2f".format(grade.score)}/${"%.2f".format(grade.maxScore)}")
            }
        }
    }

    private fun humanDate(date: String): String {
        val parsed = runCatching { LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
        return parsed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: date
    }

    private fun attendanceStatusLabel(status: AttendanceStatus): String = when (status) {
        AttendanceStatus.PRESENT -> "Presente"
        AttendanceStatus.LATE -> "Atrasado"
        AttendanceStatus.ABSENT -> "Ausente"
        AttendanceStatus.JUSTIFIED -> "Justificado"
        AttendanceStatus.UNMARKED -> "Sin marcar"
    }
}
