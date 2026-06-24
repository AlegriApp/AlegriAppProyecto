package com.example.myapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PuceBlue,
    onPrimary = AlegriLightSurface,
    secondary = AlegriSky,
    onSecondary = PuceBlue,
    tertiary = AlegriSuccess,
    background = AlegriLightBackground,
    onBackground = PuceBlue,
    surface = AlegriLightSurface,
    onSurface = PuceBlue,
    surfaceVariant = ColorTokens.LightSurfaceVariant,
    onSurfaceVariant = ColorTokens.LightOnSurfaceVariant,
    error = ColorTokens.Error,
    onError = AlegriLightSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = AlegriSky,
    onPrimary = PuceBlue,
    secondary = AlegriSky,
    onSecondary = PuceBlue,
    tertiary = ColorTokens.DarkSuccess,
    background = AlegriDarkBackground,
    onBackground = ColorTokens.DarkOnSurface,
    surface = AlegriDarkSurface,
    onSurface = ColorTokens.DarkOnSurface,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.DarkOnSurfaceVariant,
    error = ColorTokens.DarkError,
    onError = PuceBlue
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private object ColorTokens {
    val LightSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
    val LightOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF475569)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF1F2937)
    val DarkOnSurface = androidx.compose.ui.graphics.Color(0xFFE5E7EB)
    val DarkOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
    val Error = androidx.compose.ui.graphics.Color(0xFFB3261E)
    val DarkError = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
    val DarkSuccess = androidx.compose.ui.graphics.Color(0xFF81C784)
}
