package com.jfpfinance.ui.patrimonio

import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Investimento
import com.jfpfinance.core.model.MovimentoInvestimento
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.engine.Amortizacao
import com.jfpfinance.engine.CalculadoraDeSaldos
import com.jfpfinance.engine.Faturas
import com.jfpfinance.engine.Investimentos
import com.jfpfinance.engine.Patrimonio
import com.jfpfinance.engine.PatrimonioLiquido
import com.jfpfinance.engine.PosicaoDoInvestimento
import java.math.BigDecimal
import java.time.LocalDate

/*
 * Lógica de apresentação sem dependência de Android.
 * Todos os números vêm do motor financeiro; aqui eles são apenas organizados para a tela.
 */

data class PatrimonioUiState(
    val carregando: Boolean = true,
    val hoje: LocalDate = LocalDate.now(),
    val contas: List<Conta> = emptyList(),
    val patrimonio: PatrimonioLiquido? = null,
    val bens: List<Bem> = emptyList(),
    val posicoes: List<PosicaoDoInvestimento> = emptyList(),
    val movimentos: List<MovimentoInvestimento> = emptyList(),
    val erro: String? = null
)

fun montarPatrimonio(
    hoje: LocalDate,
    contas: List<Conta>,
    cartoes: List<Cartao>,
    dividas: List<Divida>,
    bens: List<Bem>,
    investimentos: List<Investimento>,
    movimentos: List<MovimentoInvestimento>,
    operacoes: List<Operacao>
): PatrimonioUiState = PatrimonioUiState(
    carregando = false,
    hoje = hoje,
    contas = contas,
    patrimonio = Patrimonio.calcular(
        saldosDasContas = contas.map { CalculadoraDeSaldos.saldoAtual(it, operacoes, hoje) },
        bens = bens,
        investimentos = investimentos,
        saldosDevedoresDasDividas = dividas.map { Amortizacao.resumo(it).saldoDevedor },
        // Dívida total do cartão, incluindo parcelas que ainda vão vencer: a obrigação já existe.
        dividasDosCartoes = cartoes.map { Faturas.limiteUtilizado(it, operacoes) }
    ),
    bens = bens,
    posicoes = investimentos.map { Investimentos.posicao(it, movimentos) },
    movimentos = movimentos
)

/** Lê uma quantidade de cotas/ações como "10" ou "0,5". Retorna null se não for maior que zero. */
fun lerQuantidade(texto: String): BigDecimal? = try {
    BigDecimal(texto.trim().replace(',', '.')).takeIf { it.signum() > 0 }
} catch (e: NumberFormatException) {
    null
}
