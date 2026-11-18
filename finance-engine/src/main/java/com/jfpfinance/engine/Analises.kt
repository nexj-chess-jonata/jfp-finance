package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.formatarBR
import com.jfpfinance.core.model.AporteMeta
import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Investimento
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.MovimentoInvestimento
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Orcamento
import com.jfpfinance.core.model.Pessoa
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.rotulo
import com.jfpfinance.core.somar
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** Retrato dos dados do usuário em um dia. As análises só leem; nunca alteram nada. */
data class DadosFinanceiros(
    val hoje: LocalDate,
    val contas: List<Conta> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val cartoes: List<Cartao> = emptyList(),
    val orcamentos: List<Orcamento> = emptyList(),
    val metas: List<Meta> = emptyList(),
    val aportes: List<AporteMeta> = emptyList(),
    val dividas: List<Divida> = emptyList(),
    val bens: List<Bem> = emptyList(),
    val investimentos: List<Investimento> = emptyList(),
    val movimentos: List<MovimentoInvestimento> = emptyList(),
    val operacoes: List<Operacao> = emptyList(),
    val pessoas: List<Pessoa> = emptyList()
) {
    fun nomeDaPessoa(id: Long?): String =
        if (id == null) "Casa (sem pessoa)" else pessoas.firstOrNull { it.id == id }?.nome ?: "Pessoa removida"

    fun nomeDaCategoria(id: Long?): String =
        if (id == null) "Sem categoria" else categorias.firstOrNull { it.id == id }?.nome ?: "Categoria removida"
}

fun Money.dividirPor(divisor: Int): Money =
    Money(BigDecimal.valueOf(centavos).divide(BigDecimal.valueOf(divisor.toLong()), 0, RoundingMode.HALF_UP).longValueExact())

fun percentual(parte: Money, total: Money): Int? {
    if (!total.isPositivo) return null
    return BigDecimal.valueOf(parte.centavos).multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(total.centavos), 0, RoundingMode.HALF_UP).toInt()
}

/**
 * Médias mensais de receita e despesa usadas como base de indicadores e projeções.
 * Usa até [MESES] meses completos anteriores ao atual. Se não houver nenhum com dados,
 * usa o mês atual (incompleto) e marca [incompleta].
 */
data class BaseHistorica(
    val meses: List<YearMonth>,
    val receitaMedia: Money,
    val despesaMedia: Money,
    val incompleta: Boolean
) {
    val sobraMedia: Money get() = receitaMedia - despesaMedia

    fun descricao(): String = when {
        meses.isEmpty() -> "Ainda não há lançamentos confirmados para calcular médias."
        incompleta -> "Média calculada só com ${meses.first().rotulo()}, que ainda não terminou."
        meses.size == 1 -> "Média calculada com ${meses.first().rotulo()}."
        else -> "Média de ${meses.size} meses: ${meses.first().rotulo()} a ${meses.last().rotulo()}."
    }

    companion object {
        const val MESES = 3

        fun calcular(operacoes: List<Operacao>, hoje: LocalDate): BaseHistorica {
            val mesAtual = YearMonth.from(hoje)
            val primeiroComDados = operacoes
                .filter { it.situacao == SituacaoOperacao.CONFIRMADA }
                .minOfOrNull { it.data }
                ?.let { YearMonth.from(it) }
                ?: return BaseHistorica(emptyList(), Money.ZERO, Money.ZERO, incompleta = true)

            val completos = (MESES downTo 1)
                .map { mesAtual.minusMonths(it.toLong()) }
                .filter { !it.isBefore(primeiroComDados) }
            val incompleta = completos.isEmpty()
            val meses = if (incompleta) listOf(mesAtual) else completos
            val resumos = meses.map { mes ->
                val fim = if (mes == mesAtual) hoje else mes.atEndOfMonth()
                ResumoFinanceiro.doPeriodo(operacoes, mes.atDay(1), fim)
            }
            return BaseHistorica(
                meses = meses,
                receitaMedia = resumos.map { it.receitas }.somar().dividirPor(meses.size),
                despesaMedia = resumos.map { it.despesas }.somar().dividirPor(meses.size),
                incompleta = incompleta
            )
        }
    }
}

enum class Gravidade { INFO, ATENCAO, CRITICO }

enum class TipoAlerta {
    SALDO_NEGATIVO, CONTA_VENCIDA, VENCIMENTO_PROXIMO, FATURA, ORCAMENTO, ENDIVIDAMENTO,
    RESERVA_BAIXA, GASTO_INCOMUM, AUMENTO_DE_CATEGORIA, CONCENTRACAO, META
}

/** [detalhe] sempre explica de onde veio a conclusão (premissas e limites), como exige a seção 18. */
data class Alerta(
    val tipo: TipoAlerta,
    val gravidade: Gravidade,
    val titulo: String,
    val detalhe: String
)

data class Indicadores(
    val base: BaseHistorica,
    /** Quanto da receita média sobra, em %. Nulo sem receita. */
    val taxaDePoupanca: Int?,
    /** Por quantos meses o saldo das contas cobre a despesa média. Nulo sem despesa. */
    val mesesDeReserva: BigDecimal?,
    /** Soma das próximas parcelas das dívidas sobre a receita média, em %. Nulo sem receita. */
    val comprometimentoComDividas: Int?,
    val parcelasMensaisDeDividas: Money,
    val saldoDasContas: Money
)

data class Diagnostico(val indicadores: Indicadores, val alertas: List<Alerta>)

/** Inteligência analítica local (seção 28.12): regras fixas sobre os números do motor. */
object Analises {
    const val DIAS_DE_RISCO = 30L
    const val DIAS_DE_VENCIMENTO = 7L
    private val PISO_GASTO_INCOMUM = Money(5_000)
    private val PISO_AUMENTO = Money(10_000)
    private val PISO_CONCENTRACAO = Money(20_000)

    fun diagnostico(dados: DadosFinanceiros): Diagnostico {
        val indicadores = indicadores(dados)
        val alertas = buildList {
            addAll(alertasDeSaldo(dados))
            addAll(alertasDeCompromissos(dados))
            addAll(alertasDeFaturas(dados))
            addAll(alertasDeOrcamento(dados))
            addAll(alertasDeEndividamentoEReserva(indicadores))
            addAll(alertasDeGastos(dados, indicadores.base))
            addAll(alertasDeMetas(dados))
        }.sortedByDescending { it.gravidade.ordinal }
        return Diagnostico(indicadores, alertas)
    }

    fun indicadores(dados: DadosFinanceiros): Indicadores {
        val base = BaseHistorica.calcular(dados.operacoes, dados.hoje)
        val saldo = CalculadoraDeSaldos.saldoTotal(dados.contas, dados.operacoes, dados.hoje)
        val parcelas = dados.dividas.mapNotNull { Amortizacao.resumo(it).proximaParcela?.valor }.somar()
        val reserva = if (base.despesaMedia.isPositivo && !saldo.isNegativo) {
            BigDecimal.valueOf(saldo.centavos)
                .divide(BigDecimal.valueOf(base.despesaMedia.centavos), 1, RoundingMode.DOWN)
        } else {
            null
        }
        return Indicadores(
            base = base,
            taxaDePoupanca = percentual(base.sobraMedia, base.receitaMedia),
            mesesDeReserva = reserva,
            comprometimentoComDividas = percentual(parcelas, base.receitaMedia),
            parcelasMensaisDeDividas = parcelas,
            saldoDasContas = saldo
        )
    }

    private fun alertasDeSaldo(dados: DadosFinanceiros): List<Alerta> {
        val horizonte = dados.hoje.plusDays(DIAS_DE_RISCO)
        return dados.contas.mapNotNull { conta ->
            ContasAPagarEReceber.primeiroDiaNegativo(conta, dados.operacoes, dados.hoje, horizonte)?.let { dia ->
                Alerta(
                    TipoAlerta.SALDO_NEGATIVO,
                    Gravidade.CRITICO,
                    if (dia == dados.hoje) "${conta.nome} está ou fica negativa hoje"
                    else "${conta.nome} pode ficar negativa em ${dia.formatarBR()}",
                    "Considera o saldo de hoje e tudo o que está lançado como previsto até ${horizonte.formatarBR()}."
                )
            }
        }
    }

    private fun alertasDeCompromissos(dados: DadosFinanceiros): List<Alerta> {
        val pendentes = ContasAPagarEReceber.pendentes(dados.operacoes, dados.hoje, DIAS_DE_VENCIMENTO)
        fun saidas(lista: List<Operacao>) = lista.map { it.efeitoEmDespesa() }.filter { it.isPositivo }.somar()
        return buildList {
            if (pendentes.vencidos.isNotEmpty()) {
                add(
                    Alerta(
                        TipoAlerta.CONTA_VENCIDA,
                        Gravidade.ATENCAO,
                        "${pendentes.vencidos.size} lançamento(s) previsto(s) com data vencida",
                        "Despesas entre eles: ${saidas(pendentes.vencidos).formatarBRL()}. " +
                            "Confirme os que já aconteceram ou cancele os que não vão acontecer."
                    )
                )
            }
            if (pendentes.proximos.isNotEmpty()) {
                add(
                    Alerta(
                        TipoAlerta.VENCIMENTO_PROXIMO,
                        Gravidade.INFO,
                        "${pendentes.proximos.size} lançamento(s) previsto(s) nos próximos $DIAS_DE_VENCIMENTO dias",
                        "Despesas entre eles: ${saidas(pendentes.proximos).formatarBRL()}."
                    )
                )
            }
        }
    }

    private fun alertasDeFaturas(dados: DadosFinanceiros): List<Alerta> =
        dados.cartoes.mapNotNull { cartao ->
            val fatura = Faturas.listar(cartao, dados.operacoes)
                .firstOrNull { !it.quitada && it.fechadaEm(dados.hoje) } ?: return@mapNotNull null
            when {
                fatura.vencimento.isBefore(dados.hoje) -> Alerta(
                    TipoAlerta.FATURA,
                    Gravidade.CRITICO,
                    "Fatura do ${cartao.nome} vencida",
                    "Venceu em ${fatura.vencimento.formatarBR()} e faltam ${fatura.restante.formatarBRL()}. " +
                        "O aplicativo não calcula juros e multa de atraso."
                )
                !fatura.vencimento.isAfter(dados.hoje.plusDays(DIAS_DE_VENCIMENTO)) -> Alerta(
                    TipoAlerta.FATURA,
                    Gravidade.ATENCAO,
                    "Fatura do ${cartao.nome} vence em ${fatura.vencimento.formatarBR()}",
                    "Faltam ${fatura.restante.formatarBRL()}."
                )
                else -> null
            }
        }

    private fun alertasDeOrcamento(dados: DadosFinanceiros): List<Alerta> =
        Orcamentos.acompanhar(dados.orcamentos, dados.operacoes, YearMonth.from(dados.hoje)).mapNotNull { a ->
            val nome = dados.nomeDaCategoria(a.orcamento.categoriaId)
            val numeros = "Gasto ${a.realizado.formatarBRL()} de ${a.orcamento.limiteMensal.formatarBRL()}; " +
                "previsto ainda não pago ${a.comprometido.formatarBRL()}."
            when (a.situacao) {
                SituacaoDoOrcamento.ESTOURADO ->
                    Alerta(TipoAlerta.ORCAMENTO, Gravidade.ATENCAO, "Orçamento de $nome estourado", numeros)
                SituacaoDoOrcamento.ATENCAO ->
                    Alerta(TipoAlerta.ORCAMENTO, Gravidade.INFO, "Orçamento de $nome perto do limite", numeros)
                SituacaoDoOrcamento.DENTRO -> null
            }
        }

    private fun alertasDeEndividamentoEReserva(i: Indicadores): List<Alerta> = buildList {
        val comprometimento = i.comprometimentoComDividas
        if (comprometimento != null && comprometimento >= 30) {
            add(
                Alerta(
                    TipoAlerta.ENDIVIDAMENTO,
                    if (comprometimento >= 50) Gravidade.CRITICO else Gravidade.ATENCAO,
                    "Parcelas de dívidas consomem $comprometimento% da receita média",
                    "Próximas parcelas somam ${i.parcelasMensaisDeDividas.formatarBRL()}; receita média " +
                        "${i.base.receitaMedia.formatarBRL()}. ${i.base.descricao()} Não inclui faturas de cartão."
                )
            )
        }
        val reserva = i.mesesDeReserva
        if (reserva != null && reserva < BigDecimal.valueOf(3)) {
            add(
                Alerta(
                    TipoAlerta.RESERVA_BAIXA,
                    if (reserva < BigDecimal.ONE) Gravidade.ATENCAO else Gravidade.INFO,
                    "O saldo das contas cobre ${reserva.toPlainString().replace('.', ',')} mês(es) de despesas",
                    "Saldo ${i.saldoDasContas.formatarBRL()}; despesa média ${i.base.despesaMedia.formatarBRL()}. " +
                        "${i.base.descricao()} Investimentos não entram nesta conta. " +
                        "A referência de 3 meses é uma convenção comum, não uma regra."
                )
            )
        }
    }

    private fun alertasDeGastos(dados: DadosFinanceiros, base: BaseHistorica): List<Alerta> {
        val mes = YearMonth.from(dados.hoje)
        val inicio = mes.atDay(1)
        val confirmadas = dados.operacoes.filter { it.situacao == SituacaoOperacao.CONFIRMADA }
        val revertidas = confirmadas.mapNotNull { it.reverteId }.toSet()
        val despesas = confirmadas.filter {
            !it.ehReversao && it.id !in revertidas && it.efeitoEmDespesa().isPositivo && it.categoriaId != null
        }
        val doMes = despesas.filter { !it.data.isBefore(inicio) && !it.data.isAfter(dados.hoje) }
        val anteriores = despesas.filter { it.data.isBefore(inicio) && !it.data.isBefore(inicio.minusDays(90)) }
            .groupBy { it.categoriaId }

        val incomuns = doMes.mapNotNull { operacao ->
            val historico = anteriores[operacao.categoriaId].orEmpty()
            if (historico.size < 3) return@mapNotNull null
            val media = historico.map { it.valor }.somar().dividirPor(historico.size)
            if (operacao.valor >= media * 2 && operacao.valor - media >= PISO_GASTO_INCOMUM) operacao to media else null
        }.sortedByDescending { it.first.valor }.take(3).map { (operacao, media) ->
            Alerta(
                TipoAlerta.GASTO_INCOMUM,
                Gravidade.INFO,
                "Gasto acima do habitual em ${dados.nomeDaCategoria(operacao.categoriaId)}",
                "${operacao.valor.formatarBRL()} em ${operacao.data.formatarBR()}" +
                    (if (operacao.descricao.isNotBlank()) " (${operacao.descricao})" else "") +
                    ". A média dos lançamentos dessa categoria nos 90 dias anteriores foi ${media.formatarBRL()}."
            )
        }

        val resumoDoMes = ResumoFinanceiro.doPeriodo(dados.operacoes, inicio, dados.hoje)
        val aumentos = if (base.incompleta || base.meses.isEmpty()) {
            emptyList()
        } else {
            val porCategoriaNaBase = base.meses
                .map { ResumoFinanceiro.doPeriodo(dados.operacoes, it.atDay(1), it.atEndOfMonth()).despesasPorCategoria }
            resumoDoMes.despesasPorCategoria.mapNotNull { (categoria, gasto) ->
                val media = porCategoriaNaBase.map { it[categoria] ?: Money.ZERO }.somar().dividirPor(base.meses.size)
                val limite = media.vezes(BigDecimal("1.3"))
                if (media.isPositivo && gasto > limite && gasto - media >= PISO_AUMENTO) {
                    Alerta(
                        TipoAlerta.AUMENTO_DE_CATEGORIA,
                        Gravidade.INFO,
                        "${dados.nomeDaCategoria(categoria)} acima da média",
                        "Neste mês até hoje: ${gasto.formatarBRL()}. Média mensal anterior: ${media.formatarBRL()}. " +
                            base.descricao()
                    )
                } else {
                    null
                }
            }
        }

        val concentracao = resumoDoMes.despesasPorCategoria.maxByOrNull { it.value }?.let { (categoria, gasto) ->
            val parte = percentual(gasto, resumoDoMes.despesas)
            if (parte != null && parte >= 50 && resumoDoMes.despesas >= PISO_CONCENTRACAO &&
                resumoDoMes.despesasPorCategoria.size > 1
            ) {
                Alerta(
                    TipoAlerta.CONCENTRACAO,
                    Gravidade.INFO,
                    "$parte% das despesas do mês estão em ${dados.nomeDaCategoria(categoria)}",
                    "${gasto.formatarBRL()} de ${resumoDoMes.despesas.formatarBRL()} confirmados neste mês até hoje."
                )
            } else {
                null
            }
        }
        return incomuns + aumentos + listOfNotNull(concentracao)
    }

    private fun alertasDeMetas(dados: DadosFinanceiros): List<Alerta> =
        dados.metas.map { Metas.progresso(it, dados.aportes, dados.hoje) }.filter { it.prazoVencido }.map { p ->
            Alerta(
                TipoAlerta.META,
                Gravidade.INFO,
                "O prazo da meta \"${p.meta.nome}\" já passou",
                "Guardado ${p.acumulado.formatarBRL()} de ${p.meta.valorAlvo.formatarBRL()}."
            )
        }
}
