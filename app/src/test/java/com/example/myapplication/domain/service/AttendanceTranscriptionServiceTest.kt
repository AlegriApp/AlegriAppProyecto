package com.example.myapplication.domain.service

import com.example.myapplication.domain.model.AttendanceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceTranscriptionServiceTest {
    private val service = AttendanceTranscriptionService()

    @Test
    fun process_detectsFullPartialAndStatusWords() {
        val students = listOf(
            AttendanceTranscriptionStudent(1L, "Andres Loor"),
            AttendanceTranscriptionStudent(2L, "Maria Gomez"),
            AttendanceTranscriptionStudent(3L, "Pedro Zambrano")
        )

        val matches = service.process(
            transcribedText = "Andres Loor presente, Maria Gomez falto, Pedro llego tarde",
            students = students
        ).associateBy { it.studentId }

        assertEquals(AttendanceStatus.PRESENT, matches[1L]?.status)
        assertEquals(AttendanceStatus.ABSENT, matches[2L]?.status)
        assertEquals(AttendanceStatus.LATE, matches[3L]?.status)
    }

    @Test
    fun process_usesAliasesAndAccentNormalization() {
        val students = listOf(
            AttendanceTranscriptionStudent(
                id = 1L,
                fullName = "Andres Loor",
                aliases = listOf("andresito")
            )
        )

        val matches = service.process("Andresito asistio", students)

        assertEquals(1, matches.size)
        assertEquals(1L, matches.first().studentId)
        assertEquals(AttendanceStatus.PRESENT, matches.first().status)
    }

    @Test
    fun process_doesNotCreateMatchWithoutDetectedStatus() {
        val students = listOf(AttendanceTranscriptionStudent(1L, "Maria Gomez"))

        val matches = service.process("Maria Gomez", students)

        assertTrue(matches.isEmpty())
    }

    @Test
    fun mapStatus_prioritizesAbsenceBeforeVino() {
        val status = service.mapStatus("Juan no vino")

        assertEquals(AttendanceStatus.ABSENT, status)
    }

    @Test
    fun process_supportsStatusLabelsWithColon() {
        val students = listOf(
            AttendanceTranscriptionStudent(1L, "Andres Loor"),
            AttendanceTranscriptionStudent(2L, "Maria Gomez")
        )

        val matches = service.process("Presentes: Andres Loor y Maria Gomez", students)

        assertEquals(2, matches.size)
        assertTrue(matches.all { it.status == AttendanceStatus.PRESENT })
    }
}
