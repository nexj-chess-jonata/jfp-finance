package com.jfpfinance.core.model

import com.jfpfinance.core.Money
import java.time.LocalDate

enum class TipoOperacao {
    /** Dinheiro que entra em uma conta e conta como receita. */
    RECEITA,

    /** Dinheiro que sai de uma conta e conta como despesa. */
    DESPESA,

    /** Movimento entre contas próprias. Nunca é receita nem despesa. */
    TRANSFERENCIA,

    /** Compra no cartão: é despesa no dia da compra e aumenta a dívida do cartão. */
    COMPRA_CARTAO,

    /** Pagamento da fatura: tira dinheiro da conta e reduz a dívida. Não é despesa de novo. */
    PAGAMENTO_FATURA,

    /** Dinheiro devolvido em conta: reduz a despesa da categoria, não é receita. */
    REEMBOLSO,

    /** Dinheiro que sai de uma conta para um investimento. Não é despesa: o patrimônio só muda de lugar. */
    APLICACAO,

    /** Dinheiro que volta de um investimento para uma conta. Não é receita. */
    RESGATE_INVESTIMENTO,

    /** Ajustes de reconciliação: mudam o saldo, mas não são receita nem despesa. */
    AJUSTE_CREDITO,
    AJUSTE_DEBITO
}

enum class SituacaoOperacao { PREVISTA, CONFIRMADA, CANCELADA }

enum class OrigemOperacao { MANUAL, IMPORTACAO, RECORRENCIA, SISTEMA }

/**
 * Registro do livro financeiro central (seção 28.1 do Prompt-Mestre).
 *
 * [valor] é sempre positivo: o sentido (entrada ou saída) vem do [tipo].
 * Uma operação com [reverteId] preenchido é o estorno de outra operação e
 * tem todos os efeitos invertidos.
 */
data class Operacao(
    val id: String,
    val tipo: TipoOperacao,
    val data: LocalDate,
    val valor: Money,
    val moeda: String = "BRL",
    val contaId: Long? = null,
    val contaDestinoId: Long? = null,
    val cartaoId: Long? = null,
    val categoriaId: Long? = null,
    val descricao: String = "",
    val contraparte: String? = null,
    val origem: OrigemOperacao = OrigemOperacao.MANUAL,
    val situacao: SituacaoOperacao = SituacaoOperacao.CONFIRMADA,
    val reverteId: String? = null,
    /** Liga as parcelas de uma mesma compra ou as repetições de um mesmo lançamento. */
    val grupoId: String? = null,
    val parcela: Int? = null,
    val totalParcelas: Int? = null,
    /** Pessoa da família a quem o lançamento pertence. Nulo = da casa toda. */
    val pessoaId: Long? = null,
    /** De onde veio um lançamento importado (nome do arquivo). */
    val origemDetalhe: String? = null,
    val criadaEm: Long = System.currentTimeMillis()
) {
    val ehReversao: Boolean get() = reverteId != null
}
