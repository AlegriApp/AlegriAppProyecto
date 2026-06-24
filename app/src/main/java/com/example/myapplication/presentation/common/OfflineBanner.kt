package com.example.myapplication.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AlegriWarning
import java.util.concurrent.TimeUnit

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
    lastSuccessfulSyncEpochMs: Long? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AlegriWarning.copy(alpha = 0.14f)
    ) {
        Row(
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
            Column {
                Text(
                    text = "Sin conexión. Mostrando datos guardados localmente.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                lastSuccessfulSyncEpochMs?.let {
                    Text(
                        text = "Última sincronización: ${formatRelative(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatRelative(epochMs: Long): String {
    val elapsedMs = (System.currentTimeMillis() - epochMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMs)
    val days = TimeUnit.MILLISECONDS.toDays(elapsedMs)
    return when {
        minutes < 1 -> "hace instantes"
        minutes < 60 -> "hace $minutes min"
        hours < 24 -> "hace $hours h"
        else -> "hace $days día${if (days == 1L) "" else "s"}"
    }
}
