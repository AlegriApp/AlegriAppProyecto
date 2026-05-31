package com.example.myapplication.presentation.common.sync

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.sync.SyncState

/**
 * Badge visual con el estado de sincronización de un registro individual.
 *
 * Mapea [SyncState] (sealed class oficial del cronograma) a icono+color+etiqueta.
 * Pensado para mostrarse en cada fila de listas (asistencias, calificaciones, etc.).
 */
@Composable
fun SyncStatusBadge(
    state: SyncState,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    val (icon, color, label) = state.toBadgeSpec()

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            if (showLabel) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}

/** Versión que recibe directamente el literal stored ("IDLE", etc.) y un error opcional. */
@Composable
fun SyncStatusBadge(
    storedStatus: String?,
    error: String? = null,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    SyncStatusBadge(
        state = SyncState.fromStored(storedStatus, error),
        modifier = modifier,
        showLabel = showLabel
    )
}

private data class BadgeSpec(
    val icon: ImageVector,
    val color: Color,
    val label: String
)

private operator fun BadgeSpec.component1() = icon
private operator fun BadgeSpec.component2() = color
private operator fun BadgeSpec.component3() = label

private fun SyncState.toBadgeSpec(): BadgeSpec = when (this) {
    SyncState.Idle -> BadgeSpec(
        icon = Icons.Filled.HourglassEmpty,
        color = Color(0xFFFF9800), // ámbar
        label = "Pendiente"
    )
    SyncState.Sending -> BadgeSpec(
        icon = Icons.Filled.CloudUpload,
        color = Color(0xFF1976D2), // azul
        label = "Enviando"
    )
    SyncState.Success -> BadgeSpec(
        icon = Icons.Filled.Check,
        color = Color(0xFF2E7D32), // verde
        label = "Sincronizado"
    )
    is SyncState.Error -> BadgeSpec(
        icon = Icons.Filled.Error,
        color = Color(0xFFD32F2F), // rojo
        label = "Error"
    )
}
