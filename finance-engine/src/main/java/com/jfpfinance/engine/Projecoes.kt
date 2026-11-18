package com.jfpfinance.engine

import com.jfpfinance.core.Money
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Premissas de uma projeção (seção 20). As variações são em % sobre as médias históricas;
 * os valores extras são somados por mês (use negativo para simular perda de renda ou corte de despesa).
 */
data class Cenario(
    val nome: String,
    val variacaoDaReceita: BigDecimal = BigDecimal.ZERO,
    val variacaoDaDespesa: BigDecimal = BigDecimal.ZERO,
    val receitaExtraMensal: Money = Money.ZERO,
    val despesaExtraMensal: Money = Money.ZERO
) {
    companion object {
        val CONSERVADOR = Cenario("Conservador", BigDecimal("-10"), BigDecimal("10"))
        val INTERMEDIARIO = Cenario("Intermediário")
        val OTIMISTA = Cenario("Otimista", BigDecimal("5"), BigDecimal("-5"))
        val PADROES = listOf(CONSERVADOR, INTERMEDIARIO, OTIMISTA)
    }
}

data class PontoDaProjecao(
    val mes: YearMonth,
    val receitas: Money,
    val despesas: Money,
    val saldoFinal: Money
)

/** Resultado de uma simulação. Nunca é gravado e nunca se mistura com os registros reais. */
data class Projecao(
    val cenario: Cenario,
    val base: BaseHistorica,
    val saldoInicial: Money,
    val pontos: List<PontoDaProjecao>,
    val premissas: List<String>
) {
    val saldoFinal: Money get() = pontos.lastOrNull()?.saldoFinal ?: saldoInicial
    val primeiroMesNegativo: YearMonth? get() = pontos.firstOrNull { it.saldoFinal.isNegativo }?.mes
}

object Projecoes {
    const val MAX_MESES = 120

    /**
     * Projeta o saldo somado das contas pelos próximos [meses] meses, a partir do mês seguinte ao atual.
     * Cada mês repete a receita e a despesa médias do histórico, ajustadas pelo [cenario].
     */
    fun projetar(dados: DadosFinanceiros, cenario: Cenario, meses: Int): Projecao {
        require(meses in 1..MAX_MESES) { "O horizonte deve ter de 1 a $MAX_MESES meses." }
        val base = BaseHistorica.calcular(dados.operacoes, dados.hoje)
        val saldoInicial = CalculadoraDeSaldos.saldoTotal(dados.contas, dados.operacoes, dados.hoje)

        val receita = naoNegativo(ajustar(base.receitaMedia, cenario.variacaoDaReceita) + cenario.receitaExtraMensal)
        val despesa = naoNegativo(ajustar(base.despesaMedia, cenario.variacaoDaDespesa) + cenario.despesaExtraMensal)

        val mesAtual = YearMonth.from(dados.hoje)
        var saldo = saldoInicial
        val pontos = List(meses) { indice ->
            saldo += receita - despesa
            PontoDaProjecao(mesAtual.plusMonths(indice + 1L), receita, despesa, saldo)
        }
        return Projecao(cenario, base, saldoInicial, pontos, premissas(base, cenario, saldoInicial))
    }

    private fun ajustar(valor: Money, variacaoPercentual: BigDecimal): Money =
        valor.vezes(BigDecimal.ONE.add(variacaoPercentual.movePointLeft(2)))

    private fun naoNegativo(valor: Money): Money = if (valor.isNegativo) Money.ZERO else valor

    private fun premissas(base: BaseHistorica, cenario: Cenario, saldoInicial: Money): List<String> = buildList {
        add("Saldo inicial: soma das contas hoje, ${saldoInicial.formatarBRL()}.")
        add(
            "Base: receita média ${base.receitaMedia.formatarBRL()} e despesa média " +
                "${base.despesaMedia.formatarBRL()} por mês. ${base.descricao()}"
        )
        add(
            "Cenário ${cenario.nome}: receita ${comSinal(cenario.variacaoDaReceita)}% e " +
                "despesa ${comSinal(cenario.variacaoDaDespesa)}% sobre as médias."
        )
        if (!cenario.receitaExtraMensal.isZero) add("Ajuste de receita: ${cenario.receitaExtraMensal.formatarBRL()} por mês.")
        if (!cenario.despesaExtraMensal.isZero) add("Ajuste de despesa: ${cenario.despesaExtraMensal.formatarBRL()} por mês.")
        add("Todos os meses repetem os mesmos valores; não há inflação, reajustes nem sazonalidade.")
        add("Não considera rendimentos de investimentos, compras futuras no cartão nem lançamentos previstos um a um.")
        add("É uma estimativa para apoiar decisões, não uma previsão garantida.")
    }

    private fun comSinal(valor: BigDecimal): String {
        val texto = valor.stripTrailingZeros().toPlainString().replace('.', ',')
        return if (valor.signum() > 0) "+$texto" else texto
    }
}
