package com.jfpfinance.core

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.format.TextStyle
import java.util.Locale

private val LOCALE_BR: Locale = Locale.forLanguageTag("pt-BR")
private val FORMATO_DATA: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

/** 18/11/2026 */
fun LocalDate.formatarBR(): String = format(FORMATO_DATA)

/** Lê "dd/mm/aaaa". Retorna null se a data não existir (ex.: 31/02). */
fun lerDataBR(texto: String): LocalDate? = try {
    LocalDate.parse(texto.trim(), FORMATO_DATA)
} catch (e: DateTimeParseException) {
    null
}

/** novembro de 2026 */
fun YearMonth.rotulo(): String = "${month.getDisplayName(TextStyle.FULL, LOCALE_BR)} de $year"
