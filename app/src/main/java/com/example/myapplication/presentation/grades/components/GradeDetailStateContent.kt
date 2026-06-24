package com.example.myapplication.presentation.grades.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AlegriMuted
import com.example.myapplication.ui.theme.AlegriWarning
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun GradeDetailLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = "Cargando calificaciones...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GradeDetailEmptyState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    GradeDetailMessageState(
        icon = Icons.Filled.Inbox,
        iconTint = AlegriMuted,
        title = "Sin calificaciones",
        message = "Aún no hay calificaciones registradas para este estudiante en el período seleccionado.",
        actionLabel = "Actualizar",
        onAction = onRetry,
        modifier = modifier
    )
}

@Composable
fun GradeDetailErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    GradeDetailMessageState(
        icon = Icons.Filled.ErrorOutline,
        iconTint = MaterialTheme.colorScheme.error,
        title = "Ocurrió un problema",
        message = message,
        actionLabel = "Reintentar",
        onAction = onRetry,
        modifier = modifier
    )
}

@Composable
fun GradeDetailOfflineState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    GradeDetailMessageState(
        icon = Icons.Filled.CloudOff,
        iconTint = AlegriWarning,
        title = "Sin conexión",
        message = "Estás viendo datos guardados localmente. Conéctate a internet para sincronizar la información más reciente.",
        actionLabel = "Reintentar sincronización",
        onAction = onRetry,
        modifier = modifier
    )
}

@Composable
private fun GradeDetailMessageState(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = iconTint
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun GradeDetailOfflineBanner(modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        color = AlegriWarning.copy(alpha = 0.14f)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = AlegriWarning,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Sin conexión. Mostrando datos guardados localmente.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailEmptyStatePreview() {
    MyApplicationTheme { GradeDetailEmptyState() }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailLoadingStatePreview() {
    MyApplicationTheme { GradeDetailLoadingState() }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailOfflineStatePreview() {
    MyApplicationTheme { GradeDetailOfflineState() }
}

@Preview(showBackground = true)
@Composable
private fun GradeDetailErrorStatePreview() {
    MyApplicationTheme { GradeDetailErrorState(message = "No se pudo cargar la información.") }
}

@Composable
fun OutlineRetryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier) {
        Text(label)
    }
}
