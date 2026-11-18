package com.jfpfinance.ui.planejamento

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.AporteMeta
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Orcamento
import com.jfpfinance.core.model.SistemaAmortizacao
import com.jfpfinance.core.model.TipoCategoria
import com.jfpfinance.engine.AcompanhamentoDoOrcamento
import com.jfpfinance.engine.Amortizacao
import com.jfpfinance.engine.FluxoDeCaixa
import com.jfpfinance.engine.MesDoFluxo
import com.jfpfinance.engine.Metas
import com.jfpfinance.engine.Orcamentos
import com.jfpfinance.engine.ProgressoDaMeta
import com.jfpfinance.engine.RegraFinanceiraException
import com.jfpfinance.engine.ResumoDaDivida
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/*
 * Lógica de apresentação sem dependência de Android.
 * Todos os números vêm do motor financeiro; aqui eles são apenas organizados para a tela.
 */

data class OrcamentoNaTela(val acompanhamento: AcompanhamentoDoOrcamento, val nomeDaCategoria: String)

data class DividaNaTela(val divida: Divida, val resumo: ResumoDaDivida)

data class PlanejamentoUiState(
    val carregando: Boolean = true,
    val hoje: LocalDate = LocalDate.now(),
    val contas: List<Conta> = emptyList(),
    val categoriasDeDespesa: List<Categoria> = emptyList(),
    val orcamentos: List<OrcamentoNaTela> = emptyList(),
    val metas: List<ProgressoDaMeta> = emptyList(),
    val fluxo: List<MesDoFluxo> = emptyList(),
    val dividas: List<DividaNaTela> = emptyList(),
    val erro: String? = null
)

const val MESES_DO_FLUXO = 6

fun montarPlanejamento(
    hoje: LocalDate,
    contas: List<Conta>,
    categorias: List<Categoria>,
    orcamentos: List<Orcamento>,
    metas: List<Meta>,
    aportes: List<AporteMeta>,
    dividas: List<Divida>,
    operacoes: List<Operacao>
): PlanejamentoUiState {
    val nomes = categorias.associate { it.id to it.nome }
    return PlanejamentoUiState(
        carregando = false,
        hoje = hoje,
        contas = contas,
        categoriasDeDespesa = categorias.filter { it.tipo == TipoCategoria.DESPESA },
        orcamentos = Orcamentos.acompanhar(orcamentos, operacoes, YearMonth.from(hoje))
            .map { OrcamentoNaTela(it, nomes[it.orcamento.categoriaId] ?: "Categoria removida") }
            .sortedBy { it.nomeDaCategoria.lowercase() },
        metas = metas.map { Metas.progresso(it, aportes, hoje) },
        fluxo = FluxoDeCaixa.mensal(contas, operacoes, hoje, MESES_DO_FLUXO),
        dividas = dividas.map { DividaNaTela(it, Amortizacao.resumo(it)) }
    )
}

/** Lê uma taxa como "1,5" ou "1.5". Retorna null se não for um número maior ou igual a zero. */
fun lerTaxa(texto: String): BigDecimal? = try {
    BigDecimal(texto.trim().replace(',', '.')).takeIf { it.signum() >= 0 }
} catch (e: NumberFormatException) {
    null
}

/** Simulação mostrada no formulário de dívida antes de salvar. */
data class PreviaDaDivida(
    val primeiraParcela: Money,
    val ultimaParcela: Money,
    val jurosTotais: Money,
    val custoTotal: Money
)

fun simularDivida(
    valor: Money?,
    taxaMensalPercentual: BigDecimal?,
    parcelas: Int?,
    sistema: SistemaAmortizacao,
    primeiraParcela: LocalDate?
): PreviaDaDivida? {
    if (valor == null || taxaMensalPercentual == null || parcelas == null || primeiraParcela == null) return null
    return try {
        val tabela = Amortizacao.tabela(valor, taxaMensalPercentual, parcelas, sistema, primeiraParcela)
        val resumo = Amortizacao.resumo(tabela, 0)
        PreviaDaDivida(tabela.first().valor, tabela.last().valor, resumo.jurosTotais, resumo.custoTotal)
    } catch (e: RegraFinanceiraException) {
        null
    }
}
