package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.OrigemOperacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.core.somar
import java.time.LocalDate

/**
 * Saldos de uma conta em uma data (seção 28.3).
 *
 * - [atual]: saldo inicial + operações confirmadas até a data. Fato.
 * - [saidasComprometidas] e [entradasPrevistas]: o que ainda vai acontecer até o horizonte. Estimativa.
 * - [disponivel]: atual menos as saídas comprometidas.
 * - [projetado]: atual mais entradas previstas menos saídas comprometidas.
 */
data class SaldosDaConta(
    val atual: Money,
    val saidasComprometidas: Money,
    val entradasPrevistas: Money
) {
    val disponivel: Money get() = atual - saidasComprometidas
    val projetado: Money get() = atual + entradasPrevistas - saidasComprometidas
}

object CalculadoraDeSaldos {

    /** Saldo real da conta: nunca é gravado, sempre calculado a partir do histórico. */
    fun saldoAtual(conta: Conta, operacoes: Iterable<Operacao>, ate: LocalDate): Money =
        conta.saldoInicial + operacoes
            .filter { it.situacao == SituacaoOperacao.CONFIRMADA && !it.data.isAfter(ate) }
            .map { it.efeitoConferido(conta) }
            .somar()

    /**
     * Saldos com projeção até [horizonte]. Entram na projeção as operações previstas
     * (inclusive as vencidas e ainda pendentes) e as confirmadas com data futura.
     */
    fun saldos(
        conta: Conta,
        operacoes: Iterable<Operacao>,
        hoje: LocalDate,
        horizonte: LocalDate = hoje
    ): SaldosDaConta {
        require(!horizonte.isBefore(hoje)) { "O horizonte da projeção não pode ser anterior a hoje." }
        val futuros = operacoes.filter { operacao ->
            when (operacao.situacao) {
                SituacaoOperacao.PREVISTA -> !operacao.data.isAfter(horizonte)
                SituacaoOperacao.CONFIRMADA -> operacao.data.isAfter(hoje) && !operacao.data.isAfter(horizonte)
                SituacaoOperacao.CANCELADA -> false
            }
        }.map { it.efeitoConferido(conta) }

        return SaldosDaConta(
            atual = saldoAtual(conta, operacoes, hoje),
            saidasComprometidas = -futuros.filter { it.isNegativo }.somar(),
            entradasPrevistas = futuros.filter { it.isPositivo }.somar()
        )
    }

    /** Soma dos saldos reais de várias contas. Transferências entre elas se anulam. */
    fun saldoTotal(contas: Iterable<Conta>, operacoes: Iterable<Operacao>, ate: LocalDate): Money {
        val lista = operacoes.toList()
        return contas.map { saldoAtual(it, lista, ate) }.somar()
    }

    /** Dívida do cartão: compras menos pagamentos e estornos, até a data. */
    fun dividaDoCartao(cartaoId: Long, operacoes: Iterable<Operacao>, ate: LocalDate): Money =
        operacoes
            .filter { it.situacao == SituacaoOperacao.CONFIRMADA && !it.data.isAfter(ate) }
            .map { it.efeitoNaDividaDoCartao(cartaoId) }
            .somar()

    private fun Operacao.efeitoConferido(conta: Conta): Money {
        val efeito = efeitoNaConta(conta.id)
        if (!efeito.isZero && moeda != conta.moeda) {
            throw RegraFinanceiraException(
                "A operação $id está em $moeda, mas a conta ${conta.nome} é em ${conta.moeda}."
            )
        }
        return efeito
    }
}

/** Receitas e despesas de um período. Chave nula nos mapas = sem categoria. */
data class ResumoDoPeriodo(
    val inicio: LocalDate,
    val fim: LocalDate,
    val receitas: Money,
    val despesas: Money,
    val receitasPorCategoria: Map<Long?, Money>,
    val despesasPorCategoria: Map<Long?, Money>
) {
    val resultado: Money get() = receitas - despesas
}

object ResumoFinanceiro {

    /**
     * Resumo do período, incluindo o primeiro e o último dia.
     *
     * Por padrão considera só o que foi confirmado (dados reais). Para ver o previsto,
     * passe [situacao] = PREVISTA — os dois nunca são misturados no mesmo resumo.
     *
     * Transferências, pagamentos de fatura e ajustes não entram: não são receita nem despesa.
     */
    fun doPeriodo(
        operacoes: Iterable<Operacao>,
        inicio: LocalDate,
        fim: LocalDate,
        situacao: SituacaoOperacao = SituacaoOperacao.CONFIRMADA
    ): ResumoDoPeriodo {
        require(!fim.isBefore(inicio)) { "O fim do período não pode ser anterior ao início." }
        val noPeriodo = operacoes.filter {
            it.situacao == situacao && !it.data.isBefore(inicio) && !it.data.isAfter(fim)
        }
        val receitas = noPeriodo.agruparPorCategoria { it.efeitoEmReceita() }
        val despesas = noPeriodo.agruparPorCategoria { it.efeitoEmDespesa() }
        return ResumoDoPeriodo(
            inicio = inicio,
            fim = fim,
            receitas = receitas.values.somar(),
            despesas = despesas.values.somar(),
            receitasPorCategoria = receitas,
            despesasPorCategoria = despesas
        )
    }

    private fun List<Operacao>.agruparPorCategoria(efeito: (Operacao) -> Money): Map<Long?, Money> =
        groupBy { it.categoriaId }
            .mapValues { (_, grupo) -> grupo.map(efeito).somar() }
            .filterValues { !it.isZero }
}

data class ResultadoDaReconciliacao(
    val saldoCalculado: Money,
    val saldoInformado: Money,
    /** Ajuste que igualaria os saldos. Nulo quando já conferem. Só vale se o usuário aceitar. */
    val ajusteProposto: Operacao?
) {
    val diferenca: Money get() = saldoInformado - saldoCalculado
    val confere: Boolean get() = diferenca.isZero
}

object Reconciliacao {

    /**
     * Compara o saldo calculado com o saldo que o usuário vê no banco.
     * Não altera nada: devolve o ajuste proposto para o usuário decidir se registra.
     */
    fun conferir(
        conta: Conta,
        operacoes: Iterable<Operacao>,
        saldoInformado: Money,
        data: LocalDate,
        idDoAjuste: String
    ): ResultadoDaReconciliacao {
        val calculado = CalculadoraDeSaldos.saldoAtual(conta, operacoes, data)
        val diferenca = saldoInformado - calculado
        val ajuste = if (diferenca.isZero) {
            null
        } else {
            Operacao(
                id = idDoAjuste,
                tipo = if (diferenca.isPositivo) TipoOperacao.AJUSTE_CREDITO else TipoOperacao.AJUSTE_DEBITO,
                data = data,
                valor = if (diferenca.isPositivo) diferenca else -diferenca,
                moeda = conta.moeda,
                contaId = conta.id,
                descricao = "Ajuste de reconciliação",
                origem = OrigemOperacao.SISTEMA
            )
        }
        return ResultadoDaReconciliacao(calculado, saldoInformado, ajuste)
    }
}
