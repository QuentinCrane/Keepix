package com.futureape.kanleme.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val KanlemeTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif),
    )
}
