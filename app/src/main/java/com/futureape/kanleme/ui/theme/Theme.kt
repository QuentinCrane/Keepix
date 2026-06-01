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

private fun lightScheme(primary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EEFB),
    onPrimaryContainer = Color(0xFF14394C),
    secondary = Color(0xFF7CC6F2),
    onSecondary = Color(0xFF0B2D40),
    tertiary = Color(0xFF5D5AA7),
    onTertiary = Color.White,
    background = Color(0xFFF3F7FB),
    onBackground = Color(0xFF17232B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17232B),
    surfaceVariant = Color(0xFFE8F0F7),
    onSurfaceVariant = Color(0xFF5D6D78),
    outline = Color(0xFFC5D2DB),
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
    accentColor: Long = 0xFF2F6886,
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
