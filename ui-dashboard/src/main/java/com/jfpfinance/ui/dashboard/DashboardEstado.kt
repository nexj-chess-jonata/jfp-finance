package com.jfpfinance.ui.dashboard

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.somar
import com.jfpfinance.engine.CalculadoraDeSaldos
import com.jfpfinance.engine.ContasAPagarEReceber
import com.jfpfinance.engine.ResumoFinanceiro
import com.jfpfinance.engine.efeitoNaConta
import java.time.LocalDate
import java.time.YearMonth

/*
 * Lógica de apresentação sem dependência de Android.
 * Todos os números vêm do motor financeiro; aqui eles são apenas organizados para a tela.
 */

data class SaldoDaContaNaTela(
    val conta: Conta,
    val saldo: Money,
    /** Primeiro dia, nos próximos 30, em que a conta ficaria negativa. Nulo = sem risco. */
    val riscoDeNegativoEm: LocalDate?
)

data class DashboardUiState(
    val carregando: Boolean = true,
    val contas: List<SaldoDaContaNaTela> = emptyList(),
    val saldoTotal: Money = Money.ZERO,
    val receitasDoMes: Money = Money.ZERO,
    val despesasDoMes: Money = Money.ZERO,
    val resultadoDoMes: Money = Money.ZERO,
    val vencidos: Int = 0,
    val proximos: Int = 0,
    /** Soma líquida do que está previsto de hoje até 7 dias (negativo = mais a pagar que a receber). */
    val saldoDosProximos: Money = Money.ZERO,
    val saldosOcultos: Boolean = false
)

fun montarDashboard(hoje: LocalDate, contas: List<Conta>, operacoes: List<Operacao>): DashboardUiState {
    val mes = YearMonth.from(hoje)
    val resumo = ResumoFinanceiro.doPeriodo(operacoes, mes.atDay(1), mes.atEndOfMonth())
    val pendentes = ContasAPagarEReceber.pendentes(operacoes, hoje, diasAFrente = 7)
    val naTela = contas.map { conta ->
        SaldoDaContaNaTela(
            conta = conta,
            saldo = CalculadoraDeSaldos.saldoAtual(conta, operacoes, hoje),
            riscoDeNegativoEm = ContasAPagarEReceber.primeiroDiaNegativo(conta, operacoes, hoje, hoje.plusDays(30))
        )
    }
    return DashboardUiState(
        carregando = false,
        contas = naTela,
        saldoTotal = naTela.map { it.saldo }.somar(),
        receitasDoMes = resumo.receitas,
        despesasDoMes = resumo.despesas,
        resultadoDoMes = resumo.resultado,
        vencidos = pendentes.vencidos.size,
        proximos = pendentes.proximos.size,
        saldoDosProximos = pendentes.proximos
            .flatMap { operacao -> contas.map { operacao.efeitoNaConta(it.id) } }
            .somar()
    )
}
