package com.example.myapplication.presentation.grades.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.AlegriMuted
import com.example.myapplication.ui.theme.AlegriWarning
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun GradeStatusChip(
    status: GradeVisualStatus
) {
    val label = when (status) {
        GradeVisualStatus.APPROVED -> "Aprobado"
        GradeVisualStatus.AT_RISK -> "En riesgo"
        GradeVisualStatus.NOT_REGISTERED -> "Sin registrar"
    }
    val color = when (status) {
        GradeVisualStatus.APPROVED -> MaterialTheme.colorScheme.tertiary
        GradeVisualStatus.AT_RISK -> AlegriWarning
        GradeVisualStatus.NOT_REGISTERED -> AlegriMuted
    }

    AssistChip(
        onClick = { },
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.14f),
            labelColor = color
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = color.copy(alpha = 0.36f)
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun GradeStatusChipPreview() {
    MyApplicationTheme {
        GradeStatusChip(status = GradeVisualStatus.AT_RISK)
    }
}
