package com.jfpfinance.ui.movimentacoes

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Pessoa
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoCategoria
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.engine.Fatura
import com.jfpfinance.engine.Faturas
import com.jfpfinance.engine.ResumoDoPeriodo
import com.jfpfinance.engine.ResumoFinanceiro
import java.time.LocalDate
import java.time.YearMonth

/*
 * Lógica de apresentação sem dependência de Android: monta o estado das telas
 * a partir dos dados e dos cálculos do motor. Nenhum cálculo financeiro é feito aqui.
 */

data class ResumoCartao(
    val cartao: Cartao,
    val utilizado: Money,
    val disponivel: Money,
    /** Fatura mais antiga ainda não quitada, se houver. */
    val faturaAPagar: Fatura?
)

data class MovimentacoesUiState(
    val carregando: Boolean = true,
    val hoje: LocalDate = LocalDate.now(),
    val mes: YearMonth = YearMonth.now(),
    val contas: List<Conta> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val pessoas: List<Pessoa> = emptyList(),
    val cartoes: List<ResumoCartao> = emptyList(),
    val lancamentosDoMes: List<Operacao> = emptyList(),
    val pendentes: List<Operacao> = emptyList(),
    val revertidas: Set<String> = emptySet(),
    val resumoDoMes: ResumoDoPeriodo? = null,
    val erro: String? = null
)

fun montarEstado(
    hoje: LocalDate,
    mes: YearMonth,
    contas: List<Conta>,
    categorias: List<Categoria>,
    cartoes: List<Cartao>,
    operacoes: List<Operacao>,
    pessoas: List<Pessoa> = emptyList()
): MovimentacoesUiState {
    val inicio = mes.atDay(1)
    val fim = mes.atEndOfMonth()
    return MovimentacoesUiState(
        carregando = false,
        hoje = hoje,
        mes = mes,
        contas = contas,
        categorias = categorias,
        pessoas = pessoas,
        cartoes = cartoes.map { cartao ->
            ResumoCartao(
                cartao = cartao,
                utilizado = Faturas.limiteUtilizado(cartao, operacoes),
                disponivel = Faturas.limiteDisponivel(cartao, operacoes),
                faturaAPagar = Faturas.listar(cartao, operacoes).firstOrNull { !it.quitada }
            )
        },
        lancamentosDoMes = operacoes
            .filter { it.situacao != SituacaoOperacao.CANCELADA && !it.data.isBefore(inicio) && !it.data.isAfter(fim) }
            .sortedWith(compareByDescending<Operacao> { it.data }.thenByDescending { it.criadaEm }),
        pendentes = operacoes
            .filter { it.situacao == SituacaoOperacao.PREVISTA }
            .sortedBy { it.data },
        revertidas = operacoes.mapNotNull { it.reverteId }.toSet(),
        resumoDoMes = ResumoFinanceiro.doPeriodo(operacoes, inicio, fim)
    )
}

enum class TipoLancamento(val rotulo: String) {
    DESPESA("Despesa"),
    RECEITA("Receita"),
    TRANSFERENCIA("Transferência"),
    COMPRA_CARTAO("Compra no cartão")
}

/** O que o usuário preencheu no formulário de novo lançamento. */
data class NovoLancamento(
    val tipo: TipoLancamento,
    val valor: Money,
    val data: LocalDate,
    val contaId: Long? = null,
    val contaDestinoId: Long? = null,
    val cartaoId: Long? = null,
    val categoriaId: Long? = null,
    val descricao: String = "",
    /** Pessoa da família; nulo = da casa toda. */
    val pessoaId: Long? = null,
    val jaAconteceu: Boolean = true,
    /** Parcelas (compra no cartão) ou repetições mensais (receita e despesa). */
    val vezes: Int = 1
)

fun NovoLancamento.paraOperacao(id: String, agora: Long): Operacao = Operacao(
    id = id,
    tipo = when (tipo) {
        TipoLancamento.DESPESA -> TipoOperacao.DESPESA
        TipoLancamento.RECEITA -> TipoOperacao.RECEITA
        TipoLancamento.TRANSFERENCIA -> TipoOperacao.TRANSFERENCIA
        TipoLancamento.COMPRA_CARTAO -> TipoOperacao.COMPRA_CARTAO
    },
    data = data,
    valor = valor,
    contaId = if (tipo == TipoLancamento.COMPRA_CARTAO) null else contaId,
    contaDestinoId = if (tipo == TipoLancamento.TRANSFERENCIA) contaDestinoId else null,
    cartaoId = if (tipo == TipoLancamento.COMPRA_CARTAO) cartaoId else null,
    categoriaId = if (tipo == TipoLancamento.TRANSFERENCIA) null else categoriaId,
    descricao = descricao.trim(),
    pessoaId = pessoaId,
    situacao = if (jaAconteceu || tipo == TipoLancamento.COMPRA_CARTAO) {
        SituacaoOperacao.CONFIRMADA
    } else {
        SituacaoOperacao.PREVISTA
    },
    criadaEm = agora
)

/** Categorias que fazem sentido para o tipo de lançamento escolhido. */
fun categoriasPara(tipo: TipoLancamento, categorias: List<Categoria>): List<Categoria> = when (tipo) {
    TipoLancamento.RECEITA -> categorias.filter { it.tipo == TipoCategoria.RECEITA }
    TipoLancamento.DESPESA, TipoLancamento.COMPRA_CARTAO -> categorias.filter { it.tipo == TipoCategoria.DESPESA }
    TipoLancamento.TRANSFERENCIA -> emptyList()
}

/** +1 quando a operação é dinheiro entrando, -1 quando é saindo, 0 quando é neutra (transferência). */
fun sinalVisual(operacao: Operacao): Int {
    val base = when (operacao.tipo) {
        TipoOperacao.RECEITA,
        TipoOperacao.REEMBOLSO,
        TipoOperacao.RESGATE_INVESTIMENTO,
        TipoOperacao.AJUSTE_CREDITO -> 1

        TipoOperacao.DESPESA,
        TipoOperacao.COMPRA_CARTAO,
        TipoOperacao.PAGAMENTO_FATURA,
        TipoOperacao.APLICACAO,
        TipoOperacao.AJUSTE_DEBITO -> -1

        TipoOperacao.TRANSFERENCIA -> 0
    }
    return if (operacao.ehReversao) -base else base
}

fun rotuloDoTipo(tipo: TipoOperacao): String = when (tipo) {
    TipoOperacao.RECEITA -> "Receita"
    TipoOperacao.DESPESA -> "Despesa"
    TipoOperacao.TRANSFERENCIA -> "Transferência"
    TipoOperacao.COMPRA_CARTAO -> "Compra no cartão"
    TipoOperacao.PAGAMENTO_FATURA -> "Pagamento de fatura"
    TipoOperacao.REEMBOLSO -> "Reembolso"
    TipoOperacao.APLICACAO -> "Aplicação em investimento"
    TipoOperacao.RESGATE_INVESTIMENTO -> "Resgate de investimento"
    TipoOperacao.AJUSTE_CREDITO -> "Ajuste (crédito)"
    TipoOperacao.AJUSTE_DEBITO -> "Ajuste (débito)"
}
