package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.core.somar
import java.time.LocalDate

/** Gera lançamentos em série: parcelas e repetições mensais. */
object Geradores {

    /**
     * Divide [modelo] em parcelas mensais cuja soma é exatamente o valor original.
     * Cada parcela conta como despesa no seu próprio mês.
     * No cartão, todas as parcelas já nascem confirmadas (a dívida existe desde a compra);
     * fora do cartão, só a primeira mantém a situação do modelo e as demais ficam previstas.
     */
    fun parcelar(modelo: Operacao, parcelas: Int, novoId: () -> String): List<Operacao> {
        if (parcelas < 1) throw RegraFinanceiraException("O número de parcelas deve ser pelo menos 1.")
        if (modelo.tipo != TipoOperacao.COMPRA_CARTAO && modelo.tipo != TipoOperacao.DESPESA) {
            throw RegraFinanceiraException("Só despesas e compras no cartão podem ser parceladas.")
        }
        if (parcelas == 1) return listOf(modelo)
        val grupo = novoId()
        return modelo.valor.dividirEm(parcelas).mapIndexed { indice, valor ->
            modelo.copy(
                id = if (indice == 0) modelo.id else novoId(),
                data = modelo.data.plusMonths(indice.toLong()),
                valor = valor,
                situacao = when {
                    modelo.tipo == TipoOperacao.COMPRA_CARTAO -> SituacaoOperacao.CONFIRMADA
                    indice == 0 -> modelo.situacao
                    else -> SituacaoOperacao.PREVISTA
                },
                grupoId = grupo,
                parcela = indice + 1,
                totalParcelas = parcelas
            )
        }
    }

    /**
     * Repete [modelo] mensalmente por [vezes] meses, com o mesmo valor.
     * A primeira ocorrência mantém a situação do modelo; as seguintes ficam previstas.
     */
    fun repetirMensalmente(modelo: Operacao, vezes: Int, novoId: () -> String): List<Operacao> {
        if (vezes < 1) throw RegraFinanceiraException("O número de repetições deve ser pelo menos 1.")
        if (vezes == 1) return listOf(modelo)
        val grupo = novoId()
        return List(vezes) { indice ->
            modelo.copy(
                id = if (indice == 0) modelo.id else novoId(),
                data = modelo.data.plusMonths(indice.toLong()),
                situacao = if (indice == 0) modelo.situacao else SituacaoOperacao.PREVISTA,
                grupoId = grupo
            )
        }
    }
}

data class Compromissos(
    /** Previstos com data anterior a hoje e ainda não confirmados. */
    val vencidos: List<Operacao>,
    /** Previstos de hoje até o fim da janela. */
    val proximos: List<Operacao>
)

/** Contas a pagar e a receber (seção 10): são as operações previstas do livro. */
object ContasAPagarEReceber {

    fun pendentes(operacoes: Iterable<Operacao>, hoje: LocalDate, diasAFrente: Long = 7): Compromissos {
        val previstas = operacoes
            .filter { it.situacao == SituacaoOperacao.PREVISTA }
            .sortedBy { it.data }
        val limite = hoje.plusDays(diasAFrente)
        return Compromissos(
            vencidos = previstas.filter { it.data.isBefore(hoje) },
            proximos = previstas.filter { !it.data.isBefore(hoje) && !it.data.isAfter(limite) }
        )
    }

    /**
     * Primeiro dia, de hoje até [horizonte], em que o saldo projetado da conta fica negativo.
     * Compromissos vencidos e ainda pendentes são considerados como devidos hoje.
     * Retorna null se não houver risco no período.
     */
    fun primeiroDiaNegativo(
        conta: Conta,
        operacoes: Iterable<Operacao>,
        hoje: LocalDate,
        horizonte: LocalDate
    ): LocalDate? {
        val lista = operacoes.toList()
        var saldo: Money = CalculadoraDeSaldos.saldoAtual(conta, lista, hoje)
        val futurosPorDia = lista
            .filter { operacao ->
                when (operacao.situacao) {
                    SituacaoOperacao.PREVISTA -> !operacao.data.isAfter(horizonte)
                    SituacaoOperacao.CONFIRMADA -> operacao.data.isAfter(hoje) && !operacao.data.isAfter(horizonte)
                    SituacaoOperacao.CANCELADA -> false
                }
            }
            .groupBy { if (it.data.isBefore(hoje)) hoje else it.data }
            .toSortedMap()

        if (saldo.isNegativo && futurosPorDia[hoje] == null) return hoje
        for ((dia, doDia) in futurosPorDia) {
            saldo += doDia.map { it.efeitoNaConta(conta.id) }.somar()
            if (saldo.isNegativo) return dia
        }
        return null
    }
}
