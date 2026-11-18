package com.jfpfinance.core.model

import com.jfpfinance.core.Money
import java.math.BigDecimal
import java.time.LocalDate

/** Limite mensal de despesa para uma categoria (seção 12). */
data class Orcamento(
    val id: Long = 0,
    val categoriaId: Long,
    val limiteMensal: Money,
    val ativo: Boolean = true
)

enum class TipoMeta(val rotulo: String) {
    RESERVA_EMERGENCIA("Reserva de emergência"),
    IMOVEL("Compra de imóvel"),
    VEICULO("Compra de veículo"),
    VIAGEM("Viagem"),
    INVESTIMENTO("Investimentos"),
    QUITAR_DIVIDA("Quitação de dívidas"),
    INDEPENDENCIA("Independência financeira"),
    OUTRO("Outro objetivo")
}

/** Objetivo financeiro (seção 17). O prazo é opcional. */
data class Meta(
    val id: Long = 0,
    val nome: String,
    val tipo: TipoMeta,
    val valorAlvo: Money,
    val prazo: LocalDate? = null,
    val ativa: Boolean = true,
    val criadaEm: Long = System.currentTimeMillis()
)

/**
 * Valor guardado para uma meta (positivo) ou retirado dela (negativo).
 * É uma marcação de quanto do seu dinheiro está reservado para o objetivo:
 * não movimenta contas e não entra no livro financeiro.
 */
data class AporteMeta(
    val id: Long = 0,
    val metaId: Long,
    val data: LocalDate,
    val valor: Money
)

enum class SistemaAmortizacao(val rotulo: String) {
    PRICE("Price (parcelas iguais)"),
    SAC("SAC (parcelas decrescentes)")
}

/** Dívida, empréstimo ou financiamento (seção 15). A taxa é em % ao mês. */
data class Divida(
    val id: Long = 0,
    val credor: String,
    val valorOriginal: Money,
    val taxaMensalPercentual: BigDecimal,
    val parcelas: Int,
    val sistema: SistemaAmortizacao,
    val primeiraParcela: LocalDate,
    val parcelasPagas: Int = 0,
    val ativa: Boolean = true
)
