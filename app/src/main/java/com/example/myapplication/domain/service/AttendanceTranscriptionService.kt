package com.example.myapplication.domain.service

import com.example.myapplication.domain.model.AttendanceStatus
import java.text.Normalizer

data class AttendanceTranscriptionStudent(
    val id: Long,
    val fullName: String,
    val aliases: List<String> = emptyList()
)

data class AttendanceTranscriptionMatch(
    val studentId: Long,
    val studentName: String,
    val status: AttendanceStatus,
    val matchedText: String
)

class AttendanceTranscriptionService {
    fun process(
        transcribedText: String,
        students: List<AttendanceTranscriptionStudent>
    ): List<AttendanceTranscriptionMatch> {
        if (transcribedText.isBlank() || students.isEmpty()) return emptyList()

        val clauses = splitIntoClauses(transcribedText)
        val matchesByStudent = linkedMapOf<Long, AttendanceTranscriptionMatch>()

        clauses.forEach { clause ->
            val status = mapStatus(clause) ?: return@forEach
            val normalizedClause = normalize(clause)

            students
                .mapNotNull { student ->
                    matchScore(normalizedClause, student)?.let { score -> student to score }
                }
                .sortedByDescending { (_, score) -> score }
                .forEach { (student, _) ->
                    matchesByStudent.putIfAbsent(
                        student.id,
                        AttendanceTranscriptionMatch(
                            studentId = student.id,
                            studentName = student.fullName,
                            status = status,
                            matchedText = clause.trim()
                        )
                    )
                }
        }

        return matchesByStudent.values.toList()
    }

    fun mapStatus(text: String): AttendanceStatus? {
        val normalized = normalize(text)
        return when {
            containsAny(
                normalized,
                "no vino",
                "no asistio",
                "falto",
                "faltaron",
                "falta",
                "ausente",
                "ausentes"
            ) -> AttendanceStatus.ABSENT
            containsAny(normalized, "justificado", "permiso", "excusa") -> AttendanceStatus.JUSTIFIED
            containsAny(normalized, "tardanza", "tarde", "atrasado", "llego tarde") -> AttendanceStatus.LATE
            containsAny(normalized, "presente", "presentes", "asistio", "asistieron", "vino", "vinieron", "ok", "si") -> AttendanceStatus.PRESENT
            else -> null
        }
    }

    fun normalize(text: String): String {
        val withoutAccents = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return withoutAccents
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun splitIntoClauses(text: String): List<String> =
        text
            .split('\n', ',', ';', '.')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun matchScore(
        normalizedClause: String,
        student: AttendanceTranscriptionStudent
    ): Int? {
        val normalizedName = normalize(student.fullName)
        val nameParts = normalizedName.split(" ").filter { it.length >= MIN_NAME_PART_LENGTH }
        val normalizedAliases = student.aliases
            .map(::normalize)
            .filter { it.length >= MIN_NAME_PART_LENGTH }

        val candidates = buildList {
            add(normalizedName to FULL_NAME_SCORE)
            normalizedAliases.forEach { add(it to ALIAS_SCORE) }
            nameParts.firstOrNull()?.let { add(it to FIRST_NAME_SCORE) }
            nameParts.lastOrNull()?.let { add(it to LAST_NAME_SCORE) }
            nameParts.drop(1).dropLast(1).forEach { add(it to NAME_PART_SCORE) }
        }

        return candidates
            .filter { (candidate, _) -> containsWordSequence(normalizedClause, candidate) }
            .maxOfOrNull { (_, score) -> score }
    }

    private fun containsAny(text: String, vararg words: String): Boolean =
        words.any { containsWordSequence(text, normalize(it)) }

    private fun containsWordSequence(text: String, candidate: String): Boolean =
        Regex("(^|\\s)${Regex.escape(candidate)}($|\\s)").containsMatchIn(text)

    private companion object {
        const val MIN_NAME_PART_LENGTH = 3
        const val FULL_NAME_SCORE = 100
        const val ALIAS_SCORE = 90
        const val FIRST_NAME_SCORE = 70
        const val LAST_NAME_SCORE = 65
        const val NAME_PART_SCORE = 50
    }
}
