package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.SistemaAmortizacao
import com.jfpfinance.core.somar
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

data class ParcelaDaDivida(
    val numero: Int,
    val vencimento: LocalDate,
    val valor: Money,
    val juros: Money,
    val amortizacao: Money,
    /** Saldo devedor depois de paga esta parcela. */
    val saldoDevedor: Money
)

data class ResumoDaDivida(
    val custoTotal: Money,
    val jurosTotais: Money,
    val saldoDevedor: Money,
    /** Juros das parcelas que ainda faltam: o que se deixa de pagar quitando o saldo agora. */
    val jurosRestantes: Money,
    val proximaParcela: ParcelaDaDivida?,
    val parcelasPagas: Int,
    val totalDeParcelas: Int
) {
    val quitada: Boolean get() = proximaParcela == null
}

/** Tabelas de amortização (seção 28.9). A taxa é sempre em % ao mês. */
object Amortizacao {

    private val PRECISAO = MathContext.DECIMAL64
    const val MAX_PARCELAS = 600

    fun tabela(
        principal: Money,
        taxaMensalPercentual: BigDecimal,
        parcelas: Int,
        sistema: SistemaAmortizacao,
        primeiraParcela: LocalDate
    ): List<ParcelaDaDivida> {
        if (!principal.isPositivo) throw RegraFinanceiraException("O valor da dívida deve ser maior que zero.")
        if (parcelas !in 1..MAX_PARCELAS) {
            throw RegraFinanceiraException("O número de parcelas deve estar entre 1 e $MAX_PARCELAS.")
        }
        if (taxaMensalPercentual.signum() < 0) throw RegraFinanceiraException("A taxa de juros não pode ser negativa.")

        val taxa = taxaMensalPercentual.divide(BigDecimal.valueOf(100), PRECISAO)
        val amortizacoesSac = principal.dividirEm(parcelas)
        val parcelaPrice = parcelaPrice(principal, taxa, parcelas)

        var saldo = principal
        return List(parcelas) { indice ->
            val juros = saldo.vezes(taxa)
            val ultima = indice == parcelas - 1
            val amortizacao = when {
                // A última parcela sempre zera o saldo, absorvendo os centavos de arredondamento.
                ultima -> saldo
                sistema == SistemaAmortizacao.SAC -> amortizacoesSac[indice]
                else -> minOf(parcelaPrice - juros, saldo)
            }
            saldo -= amortizacao
            ParcelaDaDivida(
                numero = indice + 1,
                vencimento = primeiraParcela.plusMonths(indice.toLong()),
                valor = amortizacao + juros,
                juros = juros,
                amortizacao = amortizacao,
                saldoDevedor = saldo
            )
        }
    }

    fun tabela(divida: Divida): List<ParcelaDaDivida> = tabela(
        divida.valorOriginal, divida.taxaMensalPercentual, divida.parcelas, divida.sistema, divida.primeiraParcela
    )

    /** Parcela fixa do sistema Price: P · i · (1+i)^n / ((1+i)^n − 1). */
    private fun parcelaPrice(principal: Money, taxa: BigDecimal, parcelas: Int): Money {
        if (taxa.signum() == 0) return principal.dividirEm(parcelas).first()
        val fator = BigDecimal.ONE.add(taxa).pow(parcelas, PRECISAO)
        val coeficiente = taxa.multiply(fator, PRECISAO).divide(fator.subtract(BigDecimal.ONE), PRECISAO)
        return principal.vezes(coeficiente)
    }

    fun resumo(tabela: List<ParcelaDaDivida>, parcelasPagas: Int): ResumoDaDivida {
        val pagas = parcelasPagas.coerceIn(0, tabela.size)
        val restantes = tabela.drop(pagas)
        return ResumoDaDivida(
            custoTotal = tabela.map { it.valor }.somar(),
            jurosTotais = tabela.map { it.juros }.somar(),
            saldoDevedor = restantes.map { it.amortizacao }.somar(),
            jurosRestantes = restantes.map { it.juros }.somar(),
            proximaParcela = restantes.firstOrNull(),
            parcelasPagas = pagas,
            totalDeParcelas = tabela.size
        )
    }

    fun resumo(divida: Divida): ResumoDaDivida = resumo(tabela(divida), divida.parcelasPagas)
}

/** Calculadoras simples de juros. A taxa é em % ao período. */
object Juros {
    private val PRECISAO = MathContext.DECIMAL64

    /** Montante com juros simples: P · (1 + i·n). */
    fun simples(principal: Money, taxaPercentual: BigDecimal, periodos: Int): Money {
        val taxa = taxaPercentual.divide(BigDecimal.valueOf(100), PRECISAO)
        return principal.vezes(BigDecimal.ONE.add(taxa.multiply(BigDecimal.valueOf(periodos.toLong()), PRECISAO)))
    }

    /** Montante com juros compostos: P · (1 + i)^n. */
    fun compostos(principal: Money, taxaPercentual: BigDecimal, periodos: Int): Money {
        require(periodos >= 0) { "O número de períodos não pode ser negativo." }
        val taxa = taxaPercentual.divide(BigDecimal.valueOf(100), PRECISAO)
        return principal.vezes(BigDecimal.ONE.add(taxa).pow(periodos, PRECISAO))
    }
}
