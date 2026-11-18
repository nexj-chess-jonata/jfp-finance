package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.AporteMeta
import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Investimento
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.MovimentoInvestimento
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Orcamento
import com.jfpfinance.core.model.SistemaAmortizacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoBem
import com.jfpfinance.core.model.TipoConta
import com.jfpfinance.core.model.TipoInvestimento
import com.jfpfinance.core.model.TipoMeta
import com.jfpfinance.core.model.TipoMovimentoInvestimento
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.core.somar
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanejamentoEPatrimonioTest {

    private val corrente = Conta(id = 1, nome = "Corrente", tipo = TipoConta.CONTA_BANCARIA, saldoInicial = reais(1000))
    private val poupanca = Conta(id = 2, nome = "Poupança", tipo = TipoConta.CONTA_BANCARIA, saldoInicial = reais(500))
    private val mercado = 100L
    private val hoje = LocalDate.of(2026, 11, 10)

    private fun reais(valor: Long) = Money(valor * 100)
    private fun data(mes: Int, dia: Int) = LocalDate.of(2026, mes, dia)

    private fun op(
        id: String,
        tipo: TipoOperacao,
        quando: LocalDate,
        valor: Long,
        conta: Long? = 1,
        destino: Long? = null,
        categoria: Long? = null,
        situacao: SituacaoOperacao = SituacaoOperacao.CONFIRMADA
    ) = Operacao(
        id = id, tipo = tipo, data = quando, valor = reais(valor), contaId = conta,
        contaDestinoId = destino, categoriaId = categoria, situacao = situacao, criadaEm = 1
    )

    // ---------- Orçamentos ----------

    @Test
    fun orcamentoSeparaRealizadoComprometidoEDisponivel() {
        val orcamento = Orcamento(id = 1, categoriaId = mercado, limiteMensal = reais(1000))
        val ops = listOf(
            op("d1", TipoOperacao.DESPESA, data(11, 3), 400, categoria = mercado),
            op("d2", TipoOperacao.DESPESA, data(11, 20), 250, categoria = mercado, situacao = SituacaoOperacao.PREVISTA),
            op("outra", TipoOperacao.DESPESA, data(11, 4), 999, categoria = 999),
            op("mesPassado", TipoOperacao.DESPESA, data(10, 4), 999, categoria = mercado)
        )
        val a = Orcamentos.acompanhar(listOf(orcamento), ops, YearMonth.of(2026, 11)).single()
        assertEquals(reais(400), a.realizado)
        assertEquals(reais(250), a.comprometido)
        assertEquals(reais(350), a.disponivel)
        assertEquals(40, a.percentualUsado)
        assertEquals(SituacaoDoOrcamento.DENTRO, a.situacao)
    }

    @Test
    fun orcamentoAvisaAos80PorCentoEQuandoEstoura() {
        val orcamento = Orcamento(id = 1, categoriaId = mercado, limiteMensal = reais(1000))
        fun situacaoCom(gasto: Long) = Orcamentos.acompanhar(
            listOf(orcamento),
            listOf(op("d", TipoOperacao.DESPESA, data(11, 3), gasto, categoria = mercado)),
            YearMonth.of(2026, 11)
        ).single().situacao
        assertEquals(SituacaoDoOrcamento.DENTRO, situacaoCom(799))
        assertEquals(SituacaoDoOrcamento.ATENCAO, situacaoCom(800))
        assertEquals(SituacaoDoOrcamento.ATENCAO, situacaoCom(1000))
        assertEquals(SituacaoDoOrcamento.ESTOURADO, situacaoCom(1001))
    }

    // ---------- Metas ----------

    @Test
    fun metaCalculaProgressoEAporteNecessario() {
        val meta = Meta(id = 1, nome = "Reserva", tipo = TipoMeta.RESERVA_EMERGENCIA, valorAlvo = reais(10000), prazo = LocalDate.of(2027, 5, 10))
        val aportes = listOf(
            AporteMeta(metaId = 1, data = data(10, 1), valor = reais(3000)),
            AporteMeta(metaId = 1, data = data(11, 1), valor = reais(-500)),
            AporteMeta(metaId = 2, data = data(11, 1), valor = reais(9999))
        )
        val p = Metas.progresso(meta, aportes, hoje)
        assertEquals(reais(2500), p.acumulado)
        assertEquals(reais(7500), p.restante)
        assertEquals(25, p.percentual)
        assertEquals(6, p.mesesAtePrazo)
        assertEquals(reais(1250), p.aporteMensalNecessario)
        assertFalse(p.atingida)
    }

    @Test
    fun aporteNecessarioArredondaParaCimaEPrazoParcialContaComoMes() {
        val meta = Meta(id = 1, nome = "Viagem", tipo = TipoMeta.VIAGEM, valorAlvo = reais(100), prazo = LocalDate.of(2027, 2, 15))
        val p = Metas.progresso(meta, emptyList(), hoje)
        // 10/11 até 15/02 = 3 meses completos e mais alguns dias -> 4 meses
        assertEquals(4, p.mesesAtePrazo)
        assertEquals(reais(25), p.aporteMensalNecessario)
        val tresMeses = Metas.progresso(meta.copy(prazo = LocalDate.of(2027, 2, 10)), emptyList(), hoje)
        assertEquals(3, tresMeses.mesesAtePrazo)
        assertEquals(Money(3334), tresMeses.aporteMensalNecessario)
    }

    @Test
    fun metaAtingidaSemPrazoOuVencida() {
        val semPrazo = Meta(id = 1, nome = "Carro", tipo = TipoMeta.VEICULO, valorAlvo = reais(100))
        val atingida = Metas.progresso(semPrazo, listOf(AporteMeta(metaId = 1, data = hoje, valor = reais(150))), hoje)
        assertTrue(atingida.atingida)
        assertEquals(Money.ZERO, atingida.restante)
        assertNull(atingida.aporteMensalNecessario)
        assertNull(atingida.mesesAtePrazo)

        val vencida = Metas.progresso(semPrazo.copy(prazo = data(10, 1)), emptyList(), hoje)
        assertTrue(vencida.prazoVencido)
        assertNull(vencida.aporteMensalNecessario)
    }

    @Test
    fun simulacaoERetiradaDaMeta() {
        assertEquals(8, Metas.mesesParaAtingir(reais(7500), reais(1000)))
        assertEquals(0, Metas.mesesParaAtingir(Money.ZERO, reais(1000)))
        assertNull(Metas.mesesParaAtingir(reais(7500), Money.ZERO))
        Metas.validarAporte(reais(500), reais(-500))
        assertThrows(RegraFinanceiraException::class.java) { Metas.validarAporte(reais(500), reais(-501)) }
        assertThrows(RegraFinanceiraException::class.java) { Metas.validarAporte(reais(500), Money.ZERO) }
    }

    // ---------- Fluxo de caixa ----------

    @Test
    fun fluxoSeparaRealizadoDePrevistoEProjetaOSaldo() {
        val ops = listOf(
            op("r1", TipoOperacao.RECEITA, data(11, 5), 3000),
            op("d1", TipoOperacao.DESPESA, data(11, 6), 200),
            op("t1", TipoOperacao.TRANSFERENCIA, data(11, 7), 300, conta = 1, destino = 2),
            op("vencida", TipoOperacao.DESPESA, data(10, 28), 100, situacao = SituacaoOperacao.PREVISTA),
            op("p1", TipoOperacao.DESPESA, data(11, 25), 400, situacao = SituacaoOperacao.PREVISTA),
            op("p2", TipoOperacao.RECEITA, data(12, 5), 3000, situacao = SituacaoOperacao.PREVISTA),
            op("p3", TipoOperacao.DESPESA, data(12, 10), 1000, situacao = SituacaoOperacao.PREVISTA)
        )
        val fluxo = FluxoDeCaixa.mensal(listOf(corrente, poupanca), ops, hoje, 2)

        val novembro = fluxo[0]
        assertEquals(reais(3000), novembro.entradasRealizadas)
        assertEquals(reais(200), novembro.saidasRealizadas) // a transferência interna não aparece
        assertEquals(Money.ZERO, novembro.entradasPrevistas)
        assertEquals(reais(500), novembro.saidasPrevistas) // 400 + 100 vencidos
        // saldo hoje: 1500 + 3000 - 200 = 4300; menos 500 previstos
        assertEquals(reais(3800), novembro.saldoFinalProjetado)

        val dezembro = fluxo[1]
        assertEquals(Money.ZERO, dezembro.entradasRealizadas)
        assertEquals(reais(3000), dezembro.entradasPrevistas)
        assertEquals(reais(1000), dezembro.saidasPrevistas)
        assertEquals(reais(5800), dezembro.saldoFinalProjetado)
    }

    // ---------- Dívidas ----------

    @Test
    fun tabelaPriceTemParcelasIguaisEZeraOSaldo() {
        val tabela = Amortizacao.tabela(reais(10000), BigDecimal("1"), 12, SistemaAmortizacao.PRICE, data(12, 10))
        assertEquals(12, tabela.size)
        assertEquals(Money(88849), tabela.first().valor)
        assertEquals(reais(100), tabela.first().juros)
        assertTrue(tabela.dropLast(1).all { it.valor == Money(88849) })
        assertEquals(Money.ZERO, tabela.last().saldoDevedor)
        assertEquals(reais(10000), tabela.map { it.amortizacao }.somar())
        tabela.forEach { assertEquals(it.valor, it.juros + it.amortizacao) }
        // A última parcela difere das demais só por centavos de arredondamento.
        assertTrue(Math.abs(tabela.last().valor.centavos - 88849) <= 12)
        assertEquals(LocalDate.of(2027, 11, 10), tabela.last().vencimento)
    }

    @Test
    fun tabelaSacTemAmortizacaoConstanteEJurosDecrescentes() {
        val tabela = Amortizacao.tabela(reais(12000), BigDecimal("1"), 12, SistemaAmortizacao.SAC, data(12, 10))
        assertTrue(tabela.all { it.amortizacao == reais(1000) })
        assertEquals(reais(1120), tabela.first().valor)
        assertEquals(reais(1010), tabela.last().valor)
        assertEquals(reais(780), tabela.map { it.juros }.somar())
        assertEquals(Money.ZERO, tabela.last().saldoDevedor)
    }

    @Test
    fun taxaZeroEParcelaUnica() {
        val semJuros = Amortizacao.tabela(reais(100), BigDecimal.ZERO, 3, SistemaAmortizacao.PRICE, data(12, 10))
        assertEquals(reais(100), semJuros.map { it.valor }.somar())
        assertTrue(semJuros.all { it.juros.isZero })
        val unica = Amortizacao.tabela(reais(1000), BigDecimal("2"), 1, SistemaAmortizacao.SAC, data(12, 10))
        assertEquals(reais(1020), unica.single().valor)
    }

    @Test
    fun resumoDaDividaAcompanhaParcelasPagas() {
        val tabela = Amortizacao.tabela(reais(12000), BigDecimal("1"), 12, SistemaAmortizacao.SAC, data(12, 10))
        val inicio = Amortizacao.resumo(tabela, 0)
        assertEquals(reais(12780), inicio.custoTotal)
        assertEquals(reais(780), inicio.jurosTotais)
        assertEquals(reais(12000), inicio.saldoDevedor)
        assertEquals(1, inicio.proximaParcela?.numero)

        val meio = Amortizacao.resumo(tabela, 6)
        assertEquals(reais(6000), meio.saldoDevedor)
        assertEquals(reais(210), meio.jurosRestantes)
        assertEquals(7, meio.proximaParcela?.numero)

        val fim = Amortizacao.resumo(tabela, 12)
        assertTrue(fim.quitada)
        assertEquals(Money.ZERO, fim.saldoDevedor)
    }

    @Test
    fun dividaRecusaDadosInvalidos() {
        assertThrows(RegraFinanceiraException::class.java) {
            Amortizacao.tabela(Money.ZERO, BigDecimal("1"), 12, SistemaAmortizacao.PRICE, hoje)
        }
        assertThrows(RegraFinanceiraException::class.java) {
            Amortizacao.tabela(reais(100), BigDecimal("1"), 0, SistemaAmortizacao.PRICE, hoje)
        }
        assertThrows(RegraFinanceiraException::class.java) {
            Amortizacao.tabela(reais(100), BigDecimal("-1"), 12, SistemaAmortizacao.PRICE, hoje)
        }
    }

    @Test
    fun jurosSimplesECompostos() {
        assertEquals(reais(1120), Juros.simples(reais(1000), BigDecimal("1"), 12))
        assertEquals(Money(112683), Juros.compostos(reais(1000), BigDecimal("1"), 12))
        assertEquals(reais(1000), Juros.compostos(reais(1000), BigDecimal("1"), 0))
    }

    // ---------- Investimentos ----------

    private fun aporte(valor: Long, quantidade: String? = null) = MovimentoInvestimento(
        investimentoId = 1, tipo = TipoMovimentoInvestimento.APORTE, data = hoje,
        valor = reais(valor), quantidade = quantidade?.let { BigDecimal(it) }
    )

    @Test
    fun resgateParcialPorQuantidadeUsaPrecoMedio() {
        val acao = Investimento(id = 1, nome = "ABCD3", tipo = TipoInvestimento.ACAO, valorAtual = reais(1500))
        val compras = listOf(aporte(1000, "100"), aporte(200, "20")) // custo 1200, 120 cotas, PM 10
        val antes = Investimentos.posicao(acao, compras)
        assertEquals(reais(1200), antes.custo)
        assertEquals(reais(10), antes.precoMedio)
        assertEquals(reais(300), antes.ganhoNaoRealizado)

        val resgate = Investimentos.prepararResgate(acao, compras, hoje, reais(750), BigDecimal("60"))
        assertEquals(reais(600), resgate.custoBaixado)
        val depois = Investimentos.posicao(
            acao.copy(valorAtual = Investimentos.valorAtualApos(acao, resgate)), compras + resgate
        )
        assertEquals(reais(600), depois.custo)
        assertEquals(reais(150), depois.ganhoRealizado)
        assertEquals(reais(150), depois.ganhoNaoRealizado)
        assertEquals(0, BigDecimal("60").compareTo(depois.quantidade))
    }

    @Test
    fun resgateSemQuantidadeEhProporcionalAoValorAtual() {
        val cdb = Investimento(id = 1, nome = "CDB", tipo = TipoInvestimento.CDB, valorAtual = reais(1100))
        val movimentos = listOf(aporte(1000))
        val resgate = Investimentos.prepararResgate(cdb, movimentos, hoje, reais(550))
        assertEquals(reais(500), resgate.custoBaixado)

        val total = Investimentos.prepararResgate(cdb, movimentos, hoje, reais(1100))
        assertEquals(reais(1000), total.custoBaixado)
        val zerado = Investimentos.posicao(
            cdb.copy(valorAtual = Investimentos.valorAtualApos(cdb, total)), movimentos + total
        )
        assertEquals(Money.ZERO, zerado.custo)
        assertEquals(reais(100), zerado.ganhoRealizado)
        assertEquals(Money.ZERO, zerado.ganhoNaoRealizado)
    }

    @Test
    fun rendimentoEhGanhoRealizadoENaoMudaOCusto() {
        val fii = Investimento(id = 1, nome = "FII", tipo = TipoInvestimento.FII, valorAtual = reais(1000))
        val movimentos = listOf(
            aporte(1000, "10"),
            MovimentoInvestimento(investimentoId = 1, tipo = TipoMovimentoInvestimento.RENDIMENTO, data = hoje, valor = reais(8))
        )
        val posicao = Investimentos.posicao(fii, movimentos)
        assertEquals(reais(1000), posicao.custo)
        assertEquals(reais(8), posicao.ganhoRealizado)
        assertEquals(reais(1000), Investimentos.valorAtualApos(fii, movimentos.last()))
    }

    @Test
    fun resgateInvalidoEhRecusado() {
        val cdb = Investimento(id = 1, nome = "CDB", tipo = TipoInvestimento.CDB, valorAtual = reais(1100))
        val movimentos = listOf(aporte(1000, "10"))
        assertThrows(RegraFinanceiraException::class.java) {
            Investimentos.prepararResgate(cdb, listOf(aporte(1000)), hoje, reais(1200))
        }
        assertThrows(RegraFinanceiraException::class.java) {
            Investimentos.prepararResgate(cdb, movimentos, hoje, reais(100), BigDecimal("11"))
        }
        assertThrows(RegraFinanceiraException::class.java) {
            Investimentos.prepararResgate(cdb, movimentos, hoje, Money.ZERO)
        }
    }

    @Test
    fun aplicacaoSaiDaContaSemVirarDespesa() {
        val ops = listOf(
            op("a1", TipoOperacao.APLICACAO, data(11, 5), 400),
            op("g1", TipoOperacao.RESGATE_INVESTIMENTO, data(11, 8), 150)
        )
        assertEquals(reais(750), CalculadoraDeSaldos.saldoAtual(corrente, ops, hoje))
        val resumo = ResumoFinanceiro.doPeriodo(ops, data(11, 1), data(11, 30))
        assertEquals(Money.ZERO, resumo.despesas)
        assertEquals(Money.ZERO, resumo.receitas)
    }

    // ---------- Patrimônio ----------

    @Test
    fun patrimonioLiquidoContaCadaValorUmaVez() {
        val casa = Bem(nome = "Casa", tipo = TipoBem.IMOVEL, valorAquisicao = reais(200000), dataAquisicao = data(1, 1), valorAtual = reais(250000))
        val cdb = Investimento(id = 1, nome = "CDB", tipo = TipoInvestimento.CDB, valorAtual = reais(10000))
        val pl = Patrimonio.calcular(
            saldosDasContas = listOf(reais(3000), reais(-200)),
            bens = listOf(casa),
            investimentos = listOf(cdb),
            saldosDevedoresDasDividas = listOf(reais(150000)),
            dividasDosCartoes = listOf(reais(800), reais(-50))
        )
        assertEquals(reais(263000), pl.bruto)
        assertEquals(reais(151000), pl.passivos)
        assertEquals(reais(112000), pl.liquido)
        assertEquals(reais(200), pl.contasNegativas)
    }

    @Test
    fun aplicarEmInvestimentoNaoMudaOPatrimonio() {
        val cdb = Investimento(id = 1, nome = "CDB", tipo = TipoInvestimento.CDB, valorAtual = Money.ZERO)
        fun pl(ops: List<Operacao>, investimento: Investimento) = Patrimonio.calcular(
            listOf(CalculadoraDeSaldos.saldoAtual(corrente, ops, hoje)), emptyList(), listOf(investimento),
            emptyList(), emptyList()
        ).liquido
        val antes = pl(emptyList(), cdb)
        val movimento = aporte(400)
        val depois = pl(
            listOf(op("a1", TipoOperacao.APLICACAO, data(11, 5), 400)),
            cdb.copy(valorAtual = Investimentos.valorAtualApos(cdb, movimento))
        )
        assertEquals(antes, depois)
    }
}
