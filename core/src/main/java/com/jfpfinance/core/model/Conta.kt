package com.jfpfinance.core.model

import com.jfpfinance.core.Money

enum class TipoConta(val rotulo: String) {
    CONTA_BANCARIA("Conta bancária"),
    CARTEIRA_DIGITAL("Carteira digital"),
    DINHEIRO("Dinheiro em espécie"),
    CONTA_PAGAMENTO("Conta de pagamento"),
    OUTRA("Outra")
}

/**
 * Conta do usuário (seção 9 do Prompt-Mestre).
 *
 * Na Fase 1 o saldo atual é igual ao saldo inicial. A partir da Fase 2 o saldo
 * passa a ser calculado pelo motor financeiro (saldo inicial + operações confirmadas),
 * nunca gravado como campo solto.
 */
data class Conta(
    val id: Long = 0,
    val nome: String,
    val tipo: TipoConta,
    val saldoInicial: Money,
    val moeda: String = "BRL",
    val ativa: Boolean = true,
    val criadaEm: Long = System.currentTimeMillis()
)
