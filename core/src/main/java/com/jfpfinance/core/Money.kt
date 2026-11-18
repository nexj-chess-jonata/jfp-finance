package com.jfpfinance.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Valor monetário do JFP Finance.
 *
 * Regra do Prompt-Mestre (28.2): nunca usar ponto flutuante binário para dinheiro.
 * O valor é guardado em centavos (Long). Toda conversão e todo arredondamento
 * do aplicativo passam por esta classe — não arredondar em nenhum outro lugar.
 *
 * Arredondamento oficial: HALF_UP (metade para cima), 2 casas decimais.
 */
@JvmInline
value class Money(val centavos: Long) : Comparable<Money> {

    operator fun plus(outro: Money): Money = Money(Math.addExact(centavos, outro.centavos))

    operator fun minus(outro: Money): Money = Money(Math.subtractExact(centavos, outro.centavos))

    operator fun unaryMinus(): Money = Money(Math.negateExact(centavos))

    /** Multiplica por uma quantidade inteira (ex.: número de parcelas). */
    operator fun times(quantidade: Int): Money = Money(Math.multiplyExact(centavos, quantidade.toLong()))

    /** Multiplica por um fator decimal (ex.: taxa de juros), arredondando HALF_UP. */
    fun vezes(fator: BigDecimal): Money =
        Money(BigDecimal.valueOf(centavos).multiply(fator).setScale(0, ARREDONDAMENTO).longValueExact())

    /**
     * Divide em [partes] parcelas cuja soma é exatamente igual ao valor original.
     * Os centavos que sobram vão para as primeiras parcelas (R$ 100,00 em 3 = 33,34 + 33,33 + 33,33).
     */
    fun dividirEm(partes: Int): List<Money> {
        require(partes > 0) { "O número de partes deve ser maior que zero." }
        val base = Math.floorDiv(centavos, partes.toLong())
        val resto = Math.floorMod(centavos, partes.toLong()).toInt()
        return List(partes) { indice -> Money(if (indice < resto) base + 1 else base) }
    }

    val isZero: Boolean get() = centavos == 0L
    val isNegativo: Boolean get() = centavos < 0L
    val isPositivo: Boolean get() = centavos > 0L

    fun toBigDecimal(): BigDecimal = BigDecimal.valueOf(centavos, CASAS)

    override fun compareTo(other: Money): Int = centavos.compareTo(other.centavos)

    /** Formato brasileiro: R$ 1.234,56 (negativos como -R$ 1.234,56). */
    fun formatarBRL(): String {
        val absoluto = Math.abs(centavos)
        val reais = (absoluto / 100).toString()
        val cents = (absoluto % 100).toString().padStart(2, '0')
        val comMilhar = reais.reversed().chunked(3).joinToString(".").reversed()
        val sinal = if (centavos < 0) "-" else ""
        return "${sinal}R$ $comMilhar,$cents"
    }

    override fun toString(): String = formatarBRL()

    companion object {
        private const val CASAS = 2
        val ARREDONDAMENTO: RoundingMode = RoundingMode.HALF_UP
        val ZERO = Money(0L)

        fun deCentavos(centavos: Long): Money = Money(centavos)

        fun deDecimal(valor: BigDecimal): Money =
            Money(valor.setScale(CASAS, ARREDONDAMENTO).movePointRight(CASAS).longValueExact())

        /**
         * Lê o que o usuário digitou: "1.234,56", "1234,56", "1234.56", "R$ 50".
         * Retorna null se o texto não for um valor válido.
         */
        fun deTexto(texto: String): Money? {
            val limpo = texto.replace("R$", "").replace(" ", "").trim()
            if (limpo.isEmpty()) return null
            val normalizado = when {
                limpo.contains(',') -> limpo.replace(".", "").replace(',', '.')
                else -> limpo
            }
            return try {
                deDecimal(BigDecimal(normalizado))
            } catch (e: NumberFormatException) {
                null
            } catch (e: ArithmeticException) {
                null
            }
        }
    }
}

fun Iterable<Money>.somar(): Money = fold(Money.ZERO) { acumulado, valor -> acumulado + valor }
