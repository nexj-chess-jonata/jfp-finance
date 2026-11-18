package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.AporteMeta
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Orcamento
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.somar
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// ---------------------------------------------------------------- Orçamentos

enum class SituacaoDoOrcamento { DENTRO, ATENCAO, ESTOURADO }

/**
 * Orçamento de uma categoria em um mês (seção 12).
 * [realizado] = despesas confirmadas; [comprometido] = despesas previstas ainda não pagas.
 */
data class AcompanhamentoDoOrcamento(
    val orcamento: Orcamento,
    val realizado: Money,
    val comprometido: Money
) {
    val disponivel: Money get() = orcamento.limiteMensal - realizado - comprometido

    /** Percentual do limite já gasto (só o realizado). */
    val percentualUsado: Int
        get() {
            val limite = orcamento.limiteMensal.centavos
            if (limite <= 0L) return if (realizado.isPositivo) 100 else 0
            return BigDecimal.valueOf(realizado.centavos)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(limite), 0, RoundingMode.HALF_UP)
                .toInt()
        }

    val situacao: SituacaoDoOrcamento
        get() {
            // Comparação exata em centavos: o percentual arredondado é só para exibição.
            val atingiuAtencao = Math.multiplyExact(realizado.centavos, 100L) >=
                Math.multiplyExact(orcamento.limiteMensal.centavos, Orcamentos.PERCENTUAL_DE_ATENCAO.toLong())
            return when {
                realizado > orcamento.limiteMensal -> SituacaoDoOrcamento.ESTOURADO
                atingiuAtencao || disponivel.isNegativo -> SituacaoDoOrcamento.ATENCAO
                else -> SituacaoDoOrcamento.DENTRO
            }
        }
}

object Orcamentos {
    const val PERCENTUAL_DE_ATENCAO = 80

    fun acompanhar(
        orcamentos: Iterable<Orcamento>,
        operacoes: Iterable<Operacao>,
        mes: YearMonth
    ): List<AcompanhamentoDoOrcamento> {
        val lista = operacoes.toList()
        val confirmado = ResumoFinanceiro.doPeriodo(lista, mes.atDay(1), mes.atEndOfMonth())
        val previsto = ResumoFinanceiro.doPeriodo(lista, mes.atDay(1), mes.atEndOfMonth(), SituacaoOperacao.PREVISTA)
        return orcamentos.map { orcamento ->
            AcompanhamentoDoOrcamento(
                orcamento = orcamento,
                realizado = confirmado.despesasPorCategoria[orcamento.categoriaId] ?: Money.ZERO,
                comprometido = previsto.despesasPorCategoria[orcamento.categoriaId] ?: Money.ZERO
            )
        }
    }
}

// ---------------------------------------------------------------- Metas

data class ProgressoDaMeta(
    val meta: Meta,
    val acumulado: Money,
    /** Meses de hoje até o prazo, arredondados para cima. Nulo se a meta não tem prazo. */
    val mesesAtePrazo: Int?
) {
    val restante: Money
        get() = if (acumulado >= meta.valorAlvo) Money.ZERO else meta.valorAlvo - acumulado

    val atingida: Boolean get() = acumulado >= meta.valorAlvo

    val percentual: Int
        get() {
            val alvo = meta.valorAlvo.centavos
            if (alvo <= 0L) return 100
            return BigDecimal.valueOf(acumulado.centavos)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(alvo), 0, RoundingMode.HALF_UP)
                .toInt()
                .coerceAtLeast(0)
        }

    val prazoVencido: Boolean get() = !atingida && mesesAtePrazo == 0

    /** Quanto guardar por mês para chegar no prazo. Arredondado para cima, para não faltar centavo. */
    val aporteMensalNecessario: Money?
        get() {
            val meses = mesesAtePrazo ?: return null
            if (atingida || meses <= 0) return null
            return Money((restante.centavos + meses - 1) / meses)
        }
}

object Metas {

    fun progresso(meta: Meta, aportes: Iterable<AporteMeta>, hoje: LocalDate): ProgressoDaMeta {
        val acumulado = aportes.filter { it.metaId == meta.id }.map { it.valor }.somar()
        val meses = meta.prazo?.let { prazo ->
            if (!prazo.isAfter(hoje)) {
                0
            } else {
                val completos = ChronoUnit.MONTHS.between(hoje, prazo).toInt()
                if (hoje.plusMonths(completos.toLong()).isBefore(prazo)) completos + 1 else completos
            }
        }
        return ProgressoDaMeta(meta, acumulado, meses)
    }

    /** Simulação: em quantos meses a meta é atingida guardando [aporteMensal]. Nulo se nunca. */
    fun mesesParaAtingir(restante: Money, aporteMensal: Money): Int? {
        if (!restante.isPositivo) return 0
        if (!aporteMensal.isPositivo) return null
        return ((restante.centavos + aporteMensal.centavos - 1) / aporteMensal.centavos).toInt()
    }

    /** Uma retirada não pode ser maior que o valor guardado na meta. */
    fun validarAporte(acumulado: Money, valor: Money) {
        if (valor.isZero) throw RegraFinanceiraException("Informe um valor diferente de zero.")
        if ((acumulado + valor).isNegativo) {
            throw RegraFinanceiraException("A retirada é maior que o valor guardado na meta (${acumulado.formatarBRL()}).")
        }
    }
}

// ---------------------------------------------------------------- Fluxo de caixa

/**
 * Fluxo de caixa de um mês (seção 28.4). Realizado é fato; previsto é estimativa.
 * Movimentos entre contas próprias se anulam e não aparecem.
 */
data class MesDoFluxo(
    val mes: YearMonth,
    val entradasRealizadas: Money,
    val saidasRealizadas: Money,
    val entradasPrevistas: Money,
    val saidasPrevistas: Money,
    /** Saldo somado das contas ao fim do mês, se tudo o que está previsto acontecer. */
    val saldoFinalProjetado: Money
) {
    val resultadoRealizado: Money get() = entradasRealizadas - saidasRealizadas
    val resultadoPrevisto: Money get() = entradasPrevistas - saidasPrevistas
}

object FluxoDeCaixa {

    /**
     * Fluxo do mês atual e dos seguintes, [meses] no total.
     * Compromissos vencidos e ainda pendentes entram como previstos do mês atual.
     */
    fun mensal(
        contas: List<Conta>,
        operacoes: Iterable<Operacao>,
        hoje: LocalDate,
        meses: Int
    ): List<MesDoFluxo> {
        require(meses >= 1) { "O fluxo precisa de pelo menos um mês." }
        val lista = operacoes.toList()
        val liquidoPorOperacao: (Operacao) -> Money = { operacao ->
            contas.map { operacao.efeitoNaConta(it.id) }.somar()
        }
        val mesAtual = YearMonth.from(hoje)
        var saldo = CalculadoraDeSaldos.saldoTotal(contas, lista, hoje)

        return List(meses) { indice ->
            val mes = mesAtual.plusMonths(indice.toLong())
            val inicio = mes.atDay(1)
            val fim = mes.atEndOfMonth()
            fun noMes(data: LocalDate) = !data.isBefore(inicio) && !data.isAfter(fim)

            val realizados = if (indice == 0) {
                lista.filter { it.situacao == SituacaoOperacao.CONFIRMADA && noMes(it.data) && !it.data.isAfter(hoje) }
                    .map(liquidoPorOperacao)
            } else {
                emptyList()
            }
            val previstos = lista.filter { operacao ->
                when (operacao.situacao) {
                    SituacaoOperacao.PREVISTA ->
                        noMes(operacao.data) || (indice == 0 && operacao.data.isBefore(inicio))
                    SituacaoOperacao.CONFIRMADA -> operacao.data.isAfter(hoje) && noMes(operacao.data)
                    SituacaoOperacao.CANCELADA -> false
                }
            }.map(liquidoPorOperacao)

            saldo += previstos.somar()
            MesDoFluxo(
                mes = mes,
                entradasRealizadas = realizados.filter { it.isPositivo }.somar(),
                saidasRealizadas = -realizados.filter { it.isNegativo }.somar(),
                entradasPrevistas = previstos.filter { it.isPositivo }.somar(),
                saidasPrevistas = -previstos.filter { it.isNegativo }.somar(),
                saldoFinalProjetado = saldo
            )
        }
    }
}
