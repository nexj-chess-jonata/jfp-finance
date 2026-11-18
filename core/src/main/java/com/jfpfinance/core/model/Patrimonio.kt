package com.jfpfinance.core.model

import com.jfpfinance.core.Money
import java.math.BigDecimal
import java.time.LocalDate

enum class TipoBem(val rotulo: String) {
    IMOVEL("Imóvel"),
    VEICULO("Veículo"),
    TERRENO("Terreno"),
    EQUIPAMENTO("Equipamento"),
    BEM_PESSOAL("Bem pessoal"),
    PARTICIPACAO("Participação"),
    OUTRO("Outro ativo")
}

/** Bem do patrimônio (seção 14). [valorAtual] é uma estimativa informada pelo usuário. */
data class Bem(
    val id: Long = 0,
    val nome: String,
    val tipo: TipoBem,
    val valorAquisicao: Money,
    val dataAquisicao: LocalDate,
    val valorAtual: Money,
    val ativo: Boolean = true
)

enum class TipoInvestimento(val rotulo: String) {
    RENDA_FIXA("Renda fixa"),
    TESOURO_DIRETO("Tesouro Direto"),
    CDB("CDB"),
    LCI_LCA("LCI / LCA"),
    FUNDO("Fundo"),
    ACAO("Ação"),
    ETF("ETF"),
    FII("FII"),
    OUTRO("Outro ativo")
}

/** Investimento (seção 16). [valorAtual] é a avaliação manual mais recente. */
data class Investimento(
    val id: Long = 0,
    val nome: String,
    val tipo: TipoInvestimento,
    val valorAtual: Money = Money.ZERO,
    val ativo: Boolean = true
)

enum class TipoMovimentoInvestimento(val rotulo: String) {
    APORTE("Aporte"),
    RESGATE("Resgate"),
    RENDIMENTO("Rendimento recebido")
}

/**
 * Movimento de um investimento. [valor] é sempre positivo.
 * [quantidade] é opcional (cotas ou ações). [custoBaixado] só existe em resgates:
 * é a parte do custo de aquisição que saiu junto com o resgate.
 */
data class MovimentoInvestimento(
    val id: Long = 0,
    val investimentoId: Long,
    val tipo: TipoMovimentoInvestimento,
    val data: LocalDate,
    val valor: Money,
    val quantidade: BigDecimal? = null,
    val custoBaixado: Money = Money.ZERO
)
