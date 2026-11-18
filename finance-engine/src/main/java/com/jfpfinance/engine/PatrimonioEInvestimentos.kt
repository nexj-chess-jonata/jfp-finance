package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Investimento
import com.jfpfinance.core.model.MovimentoInvestimento
import com.jfpfinance.core.model.TipoMovimentoInvestimento
import com.jfpfinance.core.somar
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/**
 * Posição de um investimento (seção 28.8).
 * Ganho realizado = o que já virou dinheiro (resgates acima do custo + rendimentos recebidos).
 * Ganho não realizado = valor atual informado menos o custo do que ainda está aplicado. É estimativa.
 */
data class PosicaoDoInvestimento(
    val investimento: Investimento,
    val quantidade: BigDecimal,
    val custo: Money,
    val aportado: Money,
    val resgatado: Money,
    val rendimentos: Money,
    val ganhoRealizado: Money
) {
    val ganhoNaoRealizado: Money get() = investimento.valorAtual - custo

    /** Custo médio por cota/ação. Nulo quando o investimento não é controlado por quantidade. */
    val precoMedio: Money?
        get() = if (quantidade.signum() > 0) custo.vezes(BigDecimal.ONE.divide(quantidade, MathContext.DECIMAL64)) else null
}

object Investimentos {
    private val PRECISAO = MathContext.DECIMAL64

    fun posicao(investimento: Investimento, movimentos: Iterable<MovimentoInvestimento>): PosicaoDoInvestimento {
        val doInvestimento = movimentos.filter { it.investimentoId == investimento.id }
        val aportes = doInvestimento.filter { it.tipo == TipoMovimentoInvestimento.APORTE }
        val resgates = doInvestimento.filter { it.tipo == TipoMovimentoInvestimento.RESGATE }
        val rendimentos = doInvestimento.filter { it.tipo == TipoMovimentoInvestimento.RENDIMENTO }
            .map { it.valor }.somar()
        val aportado = aportes.map { it.valor }.somar()
        val resgatado = resgates.map { it.valor }.somar()
        val custoBaixado = resgates.map { it.custoBaixado }.somar()
        val quantidade = aportes.fold(BigDecimal.ZERO) { soma, m -> soma.add(m.quantidade ?: BigDecimal.ZERO) }
            .subtract(resgates.fold(BigDecimal.ZERO) { soma, m -> soma.add(m.quantidade ?: BigDecimal.ZERO) })
        return PosicaoDoInvestimento(
            investimento = investimento,
            quantidade = quantidade,
            custo = aportado - custoBaixado,
            aportado = aportado,
            resgatado = resgatado,
            rendimentos = rendimentos,
            ganhoRealizado = resgatado - custoBaixado + rendimentos
        )
    }

    /**
     * Monta um resgate calculando quanto do custo sai junto com ele.
     * Com quantidade: proporcional às cotas resgatadas (preço médio).
     * Sem quantidade: proporcional ao valor resgatado sobre o valor atual informado.
     */
    fun prepararResgate(
        investimento: Investimento,
        movimentos: Iterable<MovimentoInvestimento>,
        data: LocalDate,
        valor: Money,
        quantidade: BigDecimal? = null
    ): MovimentoInvestimento {
        if (!valor.isPositivo) throw RegraFinanceiraException("O valor do resgate deve ser maior que zero.")
        val atual = posicao(investimento, movimentos)
        val custoBaixado = if (quantidade != null) {
            if (quantidade.signum() <= 0) throw RegraFinanceiraException("A quantidade deve ser maior que zero.")
            if (quantidade > atual.quantidade) {
                throw RegraFinanceiraException("A quantidade resgatada é maior que a quantidade em carteira.")
            }
            if (quantidade.compareTo(atual.quantidade) == 0) {
                atual.custo
            } else {
                atual.custo.vezes(quantidade.divide(atual.quantidade, PRECISAO))
            }
        } else {
            val valorAtual = investimento.valorAtual
            if (valor > valorAtual) {
                throw RegraFinanceiraException(
                    "O resgate é maior que o valor atual do investimento (${valorAtual.formatarBRL()}). " +
                        "Atualize o valor atual antes de resgatar."
                )
            }
            if (valor == valorAtual) {
                atual.custo
            } else {
                atual.custo.vezes(
                    BigDecimal.valueOf(valor.centavos).divide(BigDecimal.valueOf(valorAtual.centavos), PRECISAO)
                )
            }
        }
        return MovimentoInvestimento(
            investimentoId = investimento.id,
            tipo = TipoMovimentoInvestimento.RESGATE,
            data = data,
            valor = valor,
            quantidade = quantidade,
            custoBaixado = custoBaixado
        )
    }

    /** Valor atual do investimento logo depois do movimento, até a próxima avaliação manual. */
    fun valorAtualApos(investimento: Investimento, movimento: MovimentoInvestimento): Money =
        when (movimento.tipo) {
            TipoMovimentoInvestimento.APORTE -> investimento.valorAtual + movimento.valor
            TipoMovimentoInvestimento.RESGATE ->
                if (movimento.valor >= investimento.valorAtual) Money.ZERO else investimento.valorAtual - movimento.valor
            TipoMovimentoInvestimento.RENDIMENTO -> investimento.valorAtual
        }
}

/**
 * Patrimônio líquido (seção 28.7). Cada valor entra uma única vez:
 * conta com saldo negativo conta como passivo, não como ativo negativo.
 */
data class PatrimonioLiquido(
    val contas: Money,
    val investimentos: Money,
    val bens: Money,
    val dividas: Money,
    val cartoes: Money,
    val contasNegativas: Money
) {
    val bruto: Money get() = contas + investimentos + bens
    val passivos: Money get() = dividas + cartoes + contasNegativas
    val liquido: Money get() = bruto - passivos
}

object Patrimonio {

    fun calcular(
        saldosDasContas: Iterable<Money>,
        bens: Iterable<Bem>,
        investimentos: Iterable<Investimento>,
        saldosDevedoresDasDividas: Iterable<Money>,
        dividasDosCartoes: Iterable<Money>
    ): PatrimonioLiquido {
        val saldos = saldosDasContas.toList()
        return PatrimonioLiquido(
            contas = saldos.filter { it.isPositivo }.somar(),
            investimentos = investimentos.map { it.valorAtual }.somar(),
            bens = bens.map { it.valorAtual }.somar(),
            dividas = saldosDevedoresDasDividas.filter { it.isPositivo }.somar(),
            cartoes = dividasDosCartoes.filter { it.isPositivo }.somar(),
            contasNegativas = -saldos.filter { it.isNegativo }.somar()
        )
    }
}
