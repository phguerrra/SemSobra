package com.project.semsobra.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF176B4D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3F5E5),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF496457),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE9D8),
    onSecondaryContainer = Color(0xFF062016),
    tertiary = Color(0xFF8B5E00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA5),
    onTertiaryContainer = Color(0xFF2C1B00),
    background = Color(0xFFF7FAF7),
    onBackground = Color(0xFF171D1A),
    surface = Color.White,
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFE2EAE5),
    onSurfaceVariant = Color(0xFF414944),
    outline = Color(0xFF727A75),
    outlineVariant = Color(0xFFC1C9C3),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD8B8),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513A),
    onPrimaryContainer = Color(0xFFA9F5D3),
    secondary = Color(0xFFB1CCBC),
    onSecondary = Color(0xFF1C3529),
    secondaryContainer = Color(0xFF334C3F),
    onSecondaryContainer = Color(0xFFCCE9D8),
    tertiary = Color(0xFFFFBA40),
    onTertiary = Color(0xFF493100),
    tertiaryContainer = Color(0xFF694800),
    onTertiaryContainer = Color(0xFFFFDEA5),
    background = Color(0xFF101512),
    onBackground = Color(0xFFDFE4DF),
    surface = Color(0xFF171D1A),
    onSurface = Color(0xFFDFE4DF),
    surfaceVariant = Color(0xFF414944),
    onSurfaceVariant = Color(0xFFC1C9C3),
    outline = Color(0xFF8B938E),
    outlineVariant = Color(0xFF414944),
    error = Color(0xFFFFB4AB)
)

private val SemSobraTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

private val SemSobraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun SemSobraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SemSobraTypography,
        shapes = SemSobraShapes,
        content = content
    )
}
