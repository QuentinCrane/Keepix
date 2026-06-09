package com.futureape.kanleme.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.futureape.kanleme.data.settings.ThemeMode

private val LightAccent = Color(0xFFC7ECFE)
private val LightAccentInk = Color(0xFF256E8E)

private fun lightScheme(primary: Color) = lightColorScheme(
    primary = LightAccentInk,
    onPrimary = Color.White,
    primaryContainer = LightAccent,
    onPrimaryContainer = Color(0xFF103548),
    secondary = LightAccent,
    onSecondary = Color(0xFF103548),
    secondaryContainer = Color(0xFFEAF8FF),
    onSecondaryContainer = Color(0xFF263F4D),
    tertiary = Color(0xFF8ECBEA),
    onTertiary = Color(0xFF12384A),
    tertiaryContainer = Color(0xFFF0FAFF),
    onTertiaryContainer = Color(0xFF263F4D),
    background = Color.White,
    onBackground = Color(0xFF171D21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D21),
    surfaceVariant = Color(0xFFF3F7FA),
    onSurfaceVariant = Color(0xFF606C73),
    outline = Color(0xFFD9E2E7),
    outlineVariant = Color(0xFFEAF0F4),
    inverseSurface = Color(0xFF1D252A),
    inverseOnSurface = Color.White,
)

private fun darkScheme(primary: Color) = darkColorScheme(
    primary = Color(0xFF8BD4FF),
    onPrimary = Color(0xFF001925),
    primaryContainer = Color(0xFF093246),
    onPrimaryContainer = Color(0xFFE2F6FF),
    secondary = Color(0xFF9FD7F4),
    onSecondary = Color(0xFF001925),
    secondaryContainer = Color(0xFF102B38),
    onSecondaryContainer = Color(0xFFE0F4FF),
    tertiary = Color(0xFFC7C3FF),
    onTertiary = Color(0xFF15103A),
    tertiaryContainer = Color(0xFF242044),
    onTertiaryContainer = Color(0xFFECEAFF),
    background = Color.Black,
    onBackground = Color(0xFFF4F7FA),
    surface = Color(0xFF050505),
    onSurface = Color(0xFFF4F7FA),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFD4DEE5),
    inverseSurface = Color(0xFFF4F7FA),
    inverseOnSurface = Color(0xFF101010),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF2E0000),
    errorContainer = Color(0xFF451313),
    onErrorContainer = Color(0xFFFFDADA),
    outline = Color(0xFF3C4850),
    outlineVariant = Color(0xFF222A2F),
    scrim = Color.Black,
)

@Composable
fun KanlemeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: Long = 0xFFC7ECFE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = Color(accentColor)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme(accent)
        else -> lightScheme(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KanlemeTypography,
        shapes = KanlemeShapes,
        content = content,
    )
}
