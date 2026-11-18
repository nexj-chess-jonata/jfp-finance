package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.TipoOperacao

/** Erro de regra financeira: a operação pedida deixaria o livro inconsistente. */
class RegraFinanceiraException(mensagem: String) : IllegalArgumentException(mensagem)

/*
 * Este arquivo é o único lugar que define o que cada tipo de operação faz.
 * Saldos, resumos e dívida do cartão usam estas funções; nenhum outro ponto
 * do aplicativo deve decidir se algo "soma" ou "subtrai".
 */

private fun Operacao.comSinal(base: Money): Money = if (ehReversao) -base else base

/** Quanto esta operação altera o saldo da conta informada (positivo = entra). */
fun Operacao.efeitoNaConta(conta: Long): Money {
    val base = when (tipo) {
        TipoOperacao.RECEITA,
        TipoOperacao.REEMBOLSO,
        TipoOperacao.RESGATE_INVESTIMENTO,
        TipoOperacao.AJUSTE_CREDITO -> if (contaId == conta) valor else Money.ZERO

        TipoOperacao.DESPESA,
        TipoOperacao.PAGAMENTO_FATURA,
        TipoOperacao.APLICACAO,
        TipoOperacao.AJUSTE_DEBITO -> if (contaId == conta) -valor else Money.ZERO

        TipoOperacao.TRANSFERENCIA -> when (conta) {
            contaId -> -valor
            contaDestinoId -> valor
            else -> Money.ZERO
        }

        TipoOperacao.COMPRA_CARTAO -> Money.ZERO
    }
    return comSinal(base)
}

/** Quanto esta operação altera a dívida do cartão informado (positivo = deve mais). */
fun Operacao.efeitoNaDividaDoCartao(cartao: Long): Money {
    if (cartaoId != cartao) return Money.ZERO
    val base = when (tipo) {
        TipoOperacao.COMPRA_CARTAO -> valor
        TipoOperacao.PAGAMENTO_FATURA -> -valor
        else -> Money.ZERO
    }
    return comSinal(base)
}

/** Quanto esta operação conta como receita. */
fun Operacao.efeitoEmReceita(): Money =
    comSinal(if (tipo == TipoOperacao.RECEITA) valor else Money.ZERO)

/**
 * Quanto esta operação conta como despesa.
 * A compra no cartão conta aqui, uma única vez; o pagamento da fatura não conta.
 */
fun Operacao.efeitoEmDespesa(): Money {
    val base = when (tipo) {
        TipoOperacao.DESPESA, TipoOperacao.COMPRA_CARTAO -> valor
        TipoOperacao.REEMBOLSO -> -valor
        else -> Money.ZERO
    }
    return comSinal(base)
}

/** Confere se a operação tem os campos que o seu tipo exige. */
fun Operacao.validar() {
    fun exigir(condicao: Boolean, mensagem: () -> String) {
        if (!condicao) throw RegraFinanceiraException(mensagem())
    }
    exigir(id.isNotBlank()) { "Toda operação precisa de um identificador." }
    exigir(valor.isPositivo) { "O valor da operação deve ser maior que zero." }
    exigir(moeda.isNotBlank()) { "Toda operação precisa de uma moeda." }
    when (tipo) {
        TipoOperacao.RECEITA,
        TipoOperacao.DESPESA,
        TipoOperacao.REEMBOLSO,
        TipoOperacao.APLICACAO,
        TipoOperacao.RESGATE_INVESTIMENTO,
        TipoOperacao.AJUSTE_CREDITO,
        TipoOperacao.AJUSTE_DEBITO -> {
            exigir(contaId != null) { "Informe a conta da operação." }
            exigir(contaDestinoId == null && cartaoId == null) {
                "Este tipo de operação usa apenas uma conta."
            }
        }

        TipoOperacao.TRANSFERENCIA -> {
            exigir(contaId != null && contaDestinoId != null) {
                "A transferência precisa de conta de origem e conta de destino."
            }
            exigir(contaId != contaDestinoId) { "Origem e destino da transferência devem ser contas diferentes." }
            exigir(cartaoId == null) { "Transferência não usa cartão." }
        }

        TipoOperacao.COMPRA_CARTAO -> {
            exigir(cartaoId != null) { "Informe o cartão da compra." }
            exigir(contaId == null && contaDestinoId == null) {
                "A compra no cartão não movimenta conta; isso acontece no pagamento da fatura."
            }
        }

        TipoOperacao.PAGAMENTO_FATURA -> {
            exigir(contaId != null && cartaoId != null) {
                "O pagamento de fatura precisa da conta pagadora e do cartão."
            }
            exigir(contaDestinoId == null) { "Pagamento de fatura não tem conta de destino." }
        }
    }
}
