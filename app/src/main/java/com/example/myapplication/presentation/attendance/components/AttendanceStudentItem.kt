package com.example.myapplication.presentation.attendance.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.presentation.attendance.AttendanceStatus
import com.example.myapplication.presentation.attendance.AttendanceStudentUi
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun AttendanceStudentItem(
    student: AttendanceStudentUi,
    selectedStatus: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = student.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = student.gradeSection,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttendanceActionButton(
                modifier = Modifier.weight(1f),
                text = "Presente",
                icon = Icons.Filled.CheckCircle,
                selected = selectedStatus == AttendanceStatus.PRESENT,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = { onStatusSelected(AttendanceStatus.PRESENT) }
            )
            AttendanceActionButton(
                modifier = Modifier.weight(1f),
                text = "Ausente",
                icon = Icons.Filled.Cancel,
                selected = selectedStatus == AttendanceStatus.ABSENT,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = { onStatusSelected(AttendanceStatus.ABSENT) }
            )
        }
    }
}

@Composable
private fun AttendanceActionButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) containerColor else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) contentColor.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendanceStudentItemPreview() {
    MyApplicationTheme {
        AttendanceStudentItem(
            student = AttendanceStudentUi(
                id = 1L,
                name = "María González",
                gradeSection = "5to A"
            ),
            selectedStatus = AttendanceStatus.PRESENT,
            onStatusSelected = {}
        )
    }
}
