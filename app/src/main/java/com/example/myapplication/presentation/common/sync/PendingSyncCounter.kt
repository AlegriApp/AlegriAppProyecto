package com.example.myapplication.presentation.common.sync

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Chip que muestra el conteo de registros pendientes de sincronizar.
 * Pensado para top bars / encabezados de pantalla.
 *
 * Cuando `count == 0` no se muestra nada (devuelve composable vacío).
 */
@Composable
fun PendingSyncCounter(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    Surface(
        modifier = modifier,
        color = Color(0xFFFF9800).copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CloudQueue,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "$count pendiente${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFF9800)
            )
        }
    }
}
