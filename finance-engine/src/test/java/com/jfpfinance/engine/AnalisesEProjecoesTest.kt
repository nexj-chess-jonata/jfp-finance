package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Orcamento
import com.jfpfinance.core.model.SistemaAmortizacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoCategoria
import com.jfpfinance.core.model.TipoConta
import com.jfpfinance.core.model.TipoOperacao
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalisesEProjecoesTest {

    private val hoje = LocalDate.of(2026, 11, 18)
    private val corrente = Conta(id = 1, nome = "Corrente", tipo = TipoConta.CONTA_BANCARIA, saldoInicial = reais(2000))
    private val mercado = Categoria(id = 100, nome = "Alimentação", tipo = TipoCategoria.DESPESA)
    private val lazer = Categoria(id = 101, nome = "Lazer", tipo = TipoCategoria.DESPESA)

    private fun reais(valor: Long) = Money(valor * 100)
    private fun data(mes: Int, dia: Int) = LocalDate.of(2026, mes, dia)
    private var contador = 0

    private fun op(
        tipo: TipoOperacao,
        quando: LocalDate,
        valor: Long,
        categoria: Long? = null,
        situacao: SituacaoOperacao = SituacaoOperacao.CONFIRMADA,
        cartao: Long? = null,
        descricao: String = ""
    ) = Operacao(
        id = "op-${++contador}", tipo = tipo, data = quando, valor = reais(valor),
        contaId = if (cartao == null) 1 else null, cartaoId = cartao, categoriaId = categoria,
        situacao = situacao, descricao = descricao, criadaEm = 1
    )

    /** Três meses completos: receita 5000 e despesas 4000 por mês (12 lançamentos de mercado). */
    private fun historico(): List<Operacao> = (8..10).flatMap { mes ->
        listOf(op(TipoOperacao.RECEITA, data(mes, 5), 5000)) +
            (1..4).map { semana -> op(TipoOperacao.DESPESA, data(mes, semana * 6), 500, mercado.id) } +
            op(TipoOperacao.DESPESA, data(mes, 10), 2000)
    }

    private fun dados(operacoes: List<Operacao>, dividas: List<Divida> = emptyList(), orcamentos: List<Orcamento> = emptyList(), cartoes: List<Cartao> = emptyList()) =
        DadosFinanceiros(
            hoje = hoje, contas = listOf(corrente), categorias = listOf(mercado, lazer),
            operacoes = operacoes, dividas = dividas, orcamentos = orcamentos, cartoes = cartoes
        )

    private fun tipos(diagnostico: Diagnostico) = diagnostico.alertas.map { it.tipo }

    // ---------- Base histórica ----------

    @Test
    fun baseUsaTresMesesCompletosAnteriores() {
        val base = BaseHistorica.calcular(historico(), hoje)
        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10)), base.meses)
        assertEquals(reais(5000), base.receitaMedia)
        assertEquals(reais(4000), base.despesaMedia)
        assertEquals(reais(1000), base.sobraMedia)
        assertFalse(base.incompleta)
    }

    @Test
    fun baseNaoDiluiMediaComMesesSemUso() {
        // Começou a usar em outubro: a média é só de outubro, não dividida por três.
        val ops = historico().filter { it.data.monthValue == 10 }
        val base = BaseHistorica.calcular(ops, hoje)
        assertEquals(listOf(YearMonth.of(2026, 10)), base.meses)
        assertEquals(reais(5000), base.receitaMedia)
    }

    @Test
    fun baseSemHistoricoUsaMesAtualEAvisa() {
        val ops = listOf(op(TipoOperacao.RECEITA, data(11, 5), 3000), op(TipoOperacao.DESPESA, data(11, 6), 1000))
        val base = BaseHistorica.calcular(ops, hoje)
        assertTrue(base.incompleta)
        assertEquals(reais(3000), base.receitaMedia)
        assertTrue(base.descricao().contains("ainda não terminou"))

        val vazia = BaseHistorica.calcular(emptyList(), hoje)
        assertTrue(vazia.meses.isEmpty())
        assertEquals(Money.ZERO, vazia.receitaMedia)
    }

    // ---------- Indicadores e alertas ----------

    @Test
    fun indicadoresDePoupancaReservaEDividas() {
        val divida = Divida(
            credor = "Banco", valorOriginal = reais(12000), taxaMensalPercentual = BigDecimal("1"),
            parcelas = 12, sistema = SistemaAmortizacao.SAC, primeiraParcela = data(12, 10)
        )
        val i = Analises.indicadores(dados(historico(), dividas = listOf(divida)))
        assertEquals(20, i.taxaDePoupanca)
        // saldo: 2000 + 3 x 1000 = 5000; despesa média 4000 -> 1,2 mês
        assertEquals(reais(5000), i.saldoDasContas)
        assertEquals(BigDecimal("1.2"), i.mesesDeReserva)
        assertEquals(reais(1120), i.parcelasMensaisDeDividas)
        assertEquals(22, i.comprometimentoComDividas)
    }

    @Test
    fun semDadosNaoHaAlertasNemIndicadoresInventados() {
        val d = Analises.diagnostico(DadosFinanceiros(hoje = hoje, contas = listOf(corrente)))
        assertTrue(d.alertas.isEmpty())
        assertNull(d.indicadores.taxaDePoupanca)
        assertNull(d.indicadores.mesesDeReserva)
        assertNull(d.indicadores.comprometimentoComDividas)
    }

    @Test
    fun alertaDeSaldoNegativoEDeVencidos() {
        val ops = historico() + listOf(
            op(TipoOperacao.DESPESA, data(11, 10), 300, situacao = SituacaoOperacao.PREVISTA),
            op(TipoOperacao.DESPESA, data(11, 25), 6000, situacao = SituacaoOperacao.PREVISTA)
        )
        val d = Analises.diagnostico(dados(ops))
        assertTrue(TipoAlerta.SALDO_NEGATIVO in tipos(d))
        assertTrue(TipoAlerta.CONTA_VENCIDA in tipos(d))
        assertTrue(TipoAlerta.VENCIMENTO_PROXIMO in tipos(d))
        // Os mais graves vêm primeiro.
        assertEquals(Gravidade.CRITICO, d.alertas.first().gravidade)
        assertTrue(d.alertas.first().titulo.contains("25/11/2026"))
    }

    @Test
    fun alertaDeEndividamentoPorFaixa() {
        fun dividaComParcela(valor: Long) = Divida(
            credor = "Banco", valorOriginal = reais(valor), taxaMensalPercentual = BigDecimal.ZERO,
            parcelas = 1, sistema = SistemaAmortizacao.PRICE, primeiraParcela = data(12, 10)
        )
        fun gravidade(parcela: Long) = Analises.diagnostico(dados(historico(), dividas = listOf(dividaComParcela(parcela))))
            .alertas.firstOrNull { it.tipo == TipoAlerta.ENDIVIDAMENTO }?.gravidade
        assertNull(gravidade(1400))
        assertEquals(Gravidade.ATENCAO, gravidade(1500))
        assertEquals(Gravidade.CRITICO, gravidade(2500))
    }

    @Test
    fun alertaDeReservaBaixa() {
        val d = Analises.diagnostico(dados(historico()))
        val alerta = d.alertas.first { it.tipo == TipoAlerta.RESERVA_BAIXA }
        assertEquals(Gravidade.INFO, alerta.gravidade)
        assertTrue(alerta.titulo.contains("1,2"))
    }

    @Test
    fun gastoIncomumPrecisaDeHistoricoEDeDiferencaRelevante() {
        val comum = op(TipoOperacao.DESPESA, data(11, 5), 600, mercado.id)
        val incomum = op(TipoOperacao.DESPESA, data(11, 6), 1200, mercado.id, descricao = "Churrasco")
        val semHistorico = op(TipoOperacao.DESPESA, data(11, 7), 5000, lazer.id)
        val d = Analises.diagnostico(dados(historico() + listOf(comum, incomum, semHistorico)))
        val incomuns = d.alertas.filter { it.tipo == TipoAlerta.GASTO_INCOMUM }
        assertEquals(1, incomuns.size)
        assertTrue(incomuns.single().detalhe.contains("Churrasco"))
        assertTrue(incomuns.single().detalhe.contains("R$ 500,00"))
    }

    @Test
    fun aumentoDeCategoriaEConcentracao() {
        // Média de Alimentação: 2000/mês. Neste mês já foram 2700 (> 130% e diferença >= 100).
        val ops = historico() + listOf(
            op(TipoOperacao.DESPESA, data(11, 5), 900, mercado.id),
            op(TipoOperacao.DESPESA, data(11, 9), 900, mercado.id),
            op(TipoOperacao.DESPESA, data(11, 12), 900, mercado.id),
            op(TipoOperacao.DESPESA, data(11, 13), 300, lazer.id)
        )
        val d = Analises.diagnostico(dados(ops))
        assertTrue(d.alertas.any { it.tipo == TipoAlerta.AUMENTO_DE_CATEGORIA && it.titulo.contains("Alimentação") })
        val concentracao = d.alertas.first { it.tipo == TipoAlerta.CONCENTRACAO }
        assertTrue(concentracao.titulo.startsWith("90%"))
    }

    @Test
    fun alertaDeOrcamentoEDeFatura() {
        val cartao = Cartao(id = 10, nome = "Cartão", limite = reais(5000), diaFechamento = 10, diaVencimento = 20)
        val ops = historico() + listOf(
            op(TipoOperacao.DESPESA, data(11, 5), 2500, mercado.id),
            op(TipoOperacao.COMPRA_CARTAO, data(11, 2), 400, lazer.id, cartao = 10)
        )
        val d = Analises.diagnostico(
            dados(ops, orcamentos = listOf(Orcamento(id = 1, categoriaId = mercado.id, limiteMensal = reais(2000))), cartoes = listOf(cartao))
        )
        val orcamento = d.alertas.first { it.tipo == TipoAlerta.ORCAMENTO }
        assertEquals(Gravidade.ATENCAO, orcamento.gravidade)
        assertTrue(orcamento.titulo.contains("Alimentação"))
        // Fatura fechou em 10/11 e vence em 20/11: dentro de 7 dias.
        val fatura = d.alertas.first { it.tipo == TipoAlerta.FATURA }
        assertEquals(Gravidade.ATENCAO, fatura.gravidade)
        assertTrue(fatura.titulo.contains("20/11/2026"))
    }

    // ---------- Projeções ----------

    @Test
    fun projecaoIntermediariaRepeteAMedia() {
        val p = Projecoes.projetar(dados(historico()), Cenario.INTERMEDIARIO, 6)
        assertEquals(reais(5000), p.saldoInicial)
        assertEquals(6, p.pontos.size)
        assertEquals(YearMonth.of(2026, 12), p.pontos.first().mes)
        assertEquals(reais(6000), p.pontos.first().saldoFinal)
        assertEquals(reais(11000), p.saldoFinal)
        assertNull(p.primeiroMesNegativo)
        assertTrue(p.premissas.any { it.contains("estimativa") })
    }

    @Test
    fun cenariosAlteramReceitaEDespesa() {
        val conservador = Projecoes.projetar(dados(historico()), Cenario.CONSERVADOR, 12)
        // receita 4500, despesa 4400 -> +100 por mês
        assertEquals(reais(4500), conservador.pontos.first().receitas)
        assertEquals(reais(4400), conservador.pontos.first().despesas)
        assertEquals(reais(6200), conservador.saldoFinal)

        val otimista = Projecoes.projetar(dados(historico()), Cenario.OTIMISTA, 1)
        assertEquals(reais(5250), otimista.pontos.single().receitas)
        assertEquals(reais(3800), otimista.pontos.single().despesas)
    }

    @Test
    fun simulacaoDePerdaDeRendaApontaMesNegativo() {
        val perda = Cenario("Perda de renda", receitaExtraMensal = reais(-3000))
        val p = Projecoes.projetar(dados(historico()), perda, 6)
        // +2000 - 4000 = -2000 por mês: 5000 -> 3000 -> 1000 -> -1000
        assertEquals(YearMonth.of(2027, 2), p.primeiroMesNegativo)
        assertTrue(p.premissas.any { it.contains("-R$ 3.000,00") })

        val semRenda = Projecoes.projetar(dados(historico()), Cenario("Sem renda", receitaExtraMensal = reais(-9000)), 1)
        assertEquals(Money.ZERO, semRenda.pontos.single().receitas)
    }

    @Test
    fun projecaoNaoAlteraOsDados() {
        val ops = historico()
        val antes = dados(ops)
        Projecoes.projetar(antes, Cenario.CONSERVADOR, 12)
        Analises.diagnostico(antes)
        assertEquals(dados(ops), antes)
    }
}
