package com.jfpfinance.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class ModoTema { ESCURO, CLARO, AUTOMATICO }

private val EsquemaEscuro = darkColorScheme(
    primary = DouradoEnvelhecido,
    onPrimary = AzulMarinho,
    primaryContainer = AzulMarinhoCartao,
    onPrimaryContainer = DouradoEnvelhecido,
    secondary = Marfim,
    onSecondary = AzulMarinho,
    secondaryContainer = AzulMarinhoCartao,
    onSecondaryContainer = Marfim,
    background = AzulMarinho,
    onBackground = Marfim,
    surface = AzulMarinho,
    onSurface = Marfim,
    surfaceVariant = AzulMarinhoCartao,
    onSurfaceVariant = AzulAcinzentado,
    surfaceContainerLowest = AzulMarinho,
    surfaceContainerLow = AzulMarinhoElevado,
    surfaceContainer = AzulMarinhoElevado,
    surfaceContainerHigh = AzulMarinhoCartao,
    surfaceContainerHighest = AzulMarinhoCartao,
    outline = AzulAcinzentado,
    outlineVariant = AzulMarinhoCartao,
    error = VermelhoAlerta,
    onError = AzulMarinho
)

private val EsquemaClaro = lightColorScheme(
    primary = AzulMarinho,
    onPrimary = Marfim,
    primaryContainer = MarfimEscuro,
    onPrimaryContainer = AzulMarinho,
    secondary = DouradoEscuro,
    onSecondary = Marfim,
    secondaryContainer = MarfimEscuro,
    onSecondaryContainer = AzulMarinho,
    background = Marfim,
    onBackground = AzulMarinho,
    surface = Marfim,
    onSurface = AzulMarinho,
    surfaceVariant = MarfimEscuro,
    onSurfaceVariant = CinzaTexto,
    surfaceContainerLowest = Marfim,
    surfaceContainerLow = Marfim,
    surfaceContainer = MarfimEscuro,
    surfaceContainerHigh = MarfimEscuro,
    surfaceContainerHighest = MarfimEscuro,
    outline = CinzaTexto,
    outlineVariant = MarfimEscuro,
    error = VermelhoAlertaEscuro,
    onError = Marfim
)

/** O tema escuro é a apresentação principal; claro e automático são opcionais (seção 3). */
@Composable
fun JfpFinanceTheme(
    modo: ModoTema = ModoTema.ESCURO,
    content: @Composable () -> Unit
) {
    val escuro = when (modo) {
        ModoTema.ESCURO -> true
        ModoTema.CLARO -> false
        ModoTema.AUTOMATICO -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (escuro) EsquemaEscuro else EsquemaClaro,
        typography = TipografiaJfp,
        content = content
    )
}
