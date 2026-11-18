package com.jfpfinance.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val padrao = Typography()

// Títulos e valores em serifa (tom premium); textos de leitura em sem serifa.
val TipografiaJfp = Typography(
    headlineLarge = padrao.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = padrao.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = padrao.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = padrao.titleLarge.copy(fontFamily = FontFamily.Serif),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.8.sp
    )
)
