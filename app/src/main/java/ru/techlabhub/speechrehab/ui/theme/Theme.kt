package ru.techlabhub.speechrehab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = SrGreen,
        onPrimary = Color.White,
        primaryContainer = SrGreenLight,
        secondary = SrBlue,
        onSecondary = Color.White,
        error = SrRed,
        onError = Color.White,
        background = SrSurface,
        onBackground = SrOnSurface,
        surface = Color.White,
        onSurface = SrOnSurface,
    )

private val DarkColors =
    darkColorScheme(
        primary = SrGreenLight,
        onPrimary = Color.Black,
        secondary = SrBlue,
        onSecondary = Color.White,
        error = SrRedLight,
        onError = Color.Black,
        background = Color(0xFF121212),
        onBackground = Color(0xFFEAEAEA),
        surface = Color(0xFF1E1E1E),
        onSurface = Color(0xFFEAEAEA),
    )

/**
 * Корневая тема Material 3: светлая/тёмная схема от [isSystemInDarkTheme], типографика [SpeechRehabTypography].
 */
@Composable
fun SpeechRehabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SpeechRehabTypography,
        content = content,
    )
}
