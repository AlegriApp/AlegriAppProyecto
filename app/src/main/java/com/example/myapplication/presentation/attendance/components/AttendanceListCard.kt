package com.example.myapplication.presentation.attendance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.presentation.attendance.AttendanceStatus
import com.example.myapplication.presentation.attendance.AttendanceStudentUi
import com.example.myapplication.presentation.attendance.attendanceMockUiState
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun AttendanceListCard(
    courseName: String,
    subjectName: String = "",
    students: List<AttendanceStudentUi>,
    registeredCount: Int,
    statusByStudent: Map<Long, AttendanceStatus>,
    onStatusSelected: (Long, AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subjectName.isNotBlank()) {
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Lista de estudiantes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$registeredCount / ${students.size} registrados",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            students.forEachIndexed { index, student ->
                AttendanceStudentItem(
                    student = student,
                    selectedStatus = statusByStudent[student.id] ?: AttendanceStatus.UNMARKED,
                    onStatusSelected = { status ->
                        onStatusSelected(student.id, status)
                    }
                )

                if (index != students.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendanceListCardPreview() {
    MyApplicationTheme {
        AttendanceListCard(
            courseName = "5to Grado Sección A",
            students = attendanceMockUiState().students,
            registeredCount = 2,
            statusByStudent = mapOf(
                1L to AttendanceStatus.PRESENT,
                2L to AttendanceStatus.LATE
            ),
            onStatusSelected = { _, _ -> }
        )
    }
}
