package com.jfpfinance.core.model

import com.jfpfinance.core.Money

/**
 * Cartão de crédito (seção 11).
 * [diaFechamento] e [diaVencimento] vão de 1 a 31; em meses mais curtos vale o último dia do mês.
 */
data class Cartao(
    val id: Long = 0,
    val nome: String,
    val limite: Money,
    val diaFechamento: Int,
    val diaVencimento: Int,
    val moeda: String = "BRL",
    val ativo: Boolean = true
) {
    init {
        require(diaFechamento in 1..31) { "O dia de fechamento deve estar entre 1 e 31." }
        require(diaVencimento in 1..31) { "O dia de vencimento deve estar entre 1 e 31." }
    }
}
