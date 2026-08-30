package com.example.semsobra.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFFB45116),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1CF),
    onPrimaryContainer = Color(0xFF3C1600),
    secondary = Color(0xFF765845),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC9),
    onSecondaryContainer = Color(0xFF2B170C),
    tertiary = Color(0xFF5D6336),
    onTertiary = Color.White,
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF211A16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF211A16),
    surfaceVariant = Color(0xFFF4DED1),
    onSurfaceVariant = Color(0xFF53443B),
    outline = Color(0xFF85746A),
    error = Color(0xFFB3261E)
)

@Composable
fun SemSobraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
