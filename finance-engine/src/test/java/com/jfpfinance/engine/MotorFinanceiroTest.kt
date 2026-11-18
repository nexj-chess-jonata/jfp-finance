package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoConta
import com.jfpfinance.core.model.TipoOperacao
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorFinanceiroTest {

    private val corrente = Conta(id = 1, nome = "Corrente", tipo = TipoConta.CONTA_BANCARIA, saldoInicial = reais(1000))
    private val poupanca = Conta(id = 2, nome = "Poupança", tipo = TipoConta.CONTA_BANCARIA, saldoInicial = reais(500))
    private val cartao = 10L
    private val mercado = 100L
    private val salarioCat = 200L

    private fun reais(valor: Long) = Money(valor * 100)
    private fun dia(d: Int) = LocalDate.of(2026, 11, d)

    private fun op(
        id: String,
        tipo: TipoOperacao,
        dia: Int,
        valor: Long,
        conta: Long? = null,
        destino: Long? = null,
        cartaoId: Long? = null,
        categoria: Long? = null,
        situacao: SituacaoOperacao = SituacaoOperacao.CONFIRMADA
    ) = Operacao(
        id = id, tipo = tipo, data = dia(dia), valor = reais(valor), contaId = conta,
        contaDestinoId = destino, cartaoId = cartaoId, categoriaId = categoria,
        situacao = situacao, criadaEm = 1
    )

    // ---------- Saldos ----------

    @Test
    fun saldoEhSaldoInicialMaisOperacoesConfirmadasAteAData() {
        val ops = listOf(
            op("r1", TipoOperacao.RECEITA, 5, 3000, conta = 1, categoria = salarioCat),
            op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1, categoria = mercado),
            op("d2", TipoOperacao.DESPESA, 20, 100, conta = 1, categoria = mercado)
        )
        assertEquals(reais(3800), CalculadoraDeSaldos.saldoAtual(corrente, ops, dia(10)))
        assertEquals(reais(3700), CalculadoraDeSaldos.saldoAtual(corrente, ops, dia(30)))
        assertEquals(reais(1000), CalculadoraDeSaldos.saldoAtual(corrente, ops, dia(1)))
    }

    @Test
    fun transferenciaMoveSaldoMasNaoEhReceitaNemDespesa() {
        val ops = listOf(op("t1", TipoOperacao.TRANSFERENCIA, 5, 300, conta = 1, destino = 2))
        assertEquals(reais(700), CalculadoraDeSaldos.saldoAtual(corrente, ops, dia(30)))
        assertEquals(reais(800), CalculadoraDeSaldos.saldoAtual(poupanca, ops, dia(30)))
        assertEquals(reais(1500), CalculadoraDeSaldos.saldoTotal(listOf(corrente, poupanca), ops, dia(30)))

        val resumo = ResumoFinanceiro.doPeriodo(ops, dia(1), dia(30))
        assertEquals(Money.ZERO, resumo.receitas)
        assertEquals(Money.ZERO, resumo.despesas)
    }

    @Test
    fun previstasNaoAlteramSaldoAtualMasEntramNaProjecao() {
        val ops = listOf(
            op("p1", TipoOperacao.DESPESA, 15, 400, conta = 1, situacao = SituacaoOperacao.PREVISTA),
            op("p2", TipoOperacao.RECEITA, 20, 1000, conta = 1, situacao = SituacaoOperacao.PREVISTA),
            op("p3", TipoOperacao.DESPESA, 28, 50, conta = 1, situacao = SituacaoOperacao.PREVISTA),
            op("c1", TipoOperacao.DESPESA, 12, 70, conta = 1, situacao = SituacaoOperacao.CANCELADA)
        )
        val saldos = CalculadoraDeSaldos.saldos(corrente, ops, hoje = dia(10), horizonte = dia(25))
        assertEquals(reais(1000), saldos.atual)
        assertEquals(reais(400), saldos.saidasComprometidas)
        assertEquals(reais(1000), saldos.entradasPrevistas)
        assertEquals(reais(600), saldos.disponivel)
        assertEquals(reais(1600), saldos.projetado)
    }

    @Test
    fun previstaVencidaContinuaComprometida() {
        val ops = listOf(op("p1", TipoOperacao.DESPESA, 3, 150, conta = 1, situacao = SituacaoOperacao.PREVISTA))
        val saldos = CalculadoraDeSaldos.saldos(corrente, ops, hoje = dia(10))
        assertEquals(reais(1000), saldos.atual)
        assertEquals(reais(850), saldos.disponivel)
    }

    @Test
    fun operacaoEmOutraMoedaNaContaEhRecusada() {
        val emDolar = op("r1", TipoOperacao.RECEITA, 5, 10, conta = 1).copy(moeda = "USD")
        assertThrows(RegraFinanceiraException::class.java) {
            CalculadoraDeSaldos.saldoAtual(corrente, listOf(emDolar), dia(30))
        }
    }

    // ---------- Cartão: sem dupla contabilização ----------

    @Test
    fun compraNoCartaoEhDespesaUmaUnicaVez() {
        val ops = listOf(
            op("c1", TipoOperacao.COMPRA_CARTAO, 5, 250, cartaoId = cartao, categoria = mercado),
            op("f1", TipoOperacao.PAGAMENTO_FATURA, 25, 250, conta = 1, cartaoId = cartao)
        )
        // Na compra: vira despesa e dívida, a conta não muda.
        assertEquals(reais(1000), CalculadoraDeSaldos.saldoAtual(corrente, ops, dia(10)))
        assertEquals(reais(250), CalculadoraDeSaldos.dividaDoCartao(cartao, ops, dia(10)))
        // No pagamento: a conta paga e a dívida zera.
        assertEquals(reais(750), CalculadoraDeSaldos.saldoAtual(corrente, ops, dia(30)))
        assertEquals(Money.ZERO, CalculadoraDeSaldos.dividaDoCartao(cartao, ops, dia(30)))
        // No mês inteiro, a despesa é 250 — não 500.
        val resumo = ResumoFinanceiro.doPeriodo(ops, dia(1), dia(30))
        assertEquals(reais(250), resumo.despesas)
        assertEquals(mapOf<Long?, Money>(mercado to reais(250)), resumo.despesasPorCategoria)
    }

    @Test
    fun estornoDeCompraReduzDividaEDespesa() {
        val livro = LivroFinanceiro.vazio()
            .registrar(op("c1", TipoOperacao.COMPRA_CARTAO, 5, 250, cartaoId = cartao, categoria = mercado))
            .reverter("c1", "c1-estorno", dia(8), agora = 2)
        assertEquals(Money.ZERO, CalculadoraDeSaldos.dividaDoCartao(cartao, livro.operacoes, dia(30)))
        assertEquals(Money.ZERO, ResumoFinanceiro.doPeriodo(livro.operacoes, dia(1), dia(30)).despesas)
        // Antes do estorno, a dívida existia: o histórico é preservado.
        assertEquals(reais(250), CalculadoraDeSaldos.dividaDoCartao(cartao, livro.operacoes, dia(6)))
    }

    // ---------- Resumo ----------

    @Test
    fun resumoSeparaReceitasEDespesasPorCategoria() {
        val ops = listOf(
            op("r1", TipoOperacao.RECEITA, 5, 3000, conta = 1, categoria = salarioCat),
            op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1, categoria = mercado),
            op("d2", TipoOperacao.DESPESA, 7, 80, conta = 1),
            op("x1", TipoOperacao.REEMBOLSO, 9, 50, conta = 1, categoria = mercado),
            op("a1", TipoOperacao.AJUSTE_CREDITO, 9, 10, conta = 1),
            op("p1", TipoOperacao.DESPESA, 10, 999, conta = 1, situacao = SituacaoOperacao.PREVISTA),
            op("fora", TipoOperacao.DESPESA, 30, 40, conta = 1)
        )
        val resumo = ResumoFinanceiro.doPeriodo(ops, dia(1), dia(15))
        assertEquals(reais(3000), resumo.receitas)
        assertEquals(reais(230), resumo.despesas)
        assertEquals(reais(2770), resumo.resultado)
        assertEquals(mapOf<Long?, Money>(mercado to reais(150), null to reais(80)), resumo.despesasPorCategoria)

        val previsto = ResumoFinanceiro.doPeriodo(ops, dia(1), dia(15), SituacaoOperacao.PREVISTA)
        assertEquals(reais(999), previsto.despesas)
    }

    @Test
    fun reembolsoAumentaSaldoSemVirarReceita() {
        val ops = listOf(op("x1", TipoOperacao.REEMBOLSO, 9, 50, conta = 1, categoria = mercado))
        assertEquals(reais(1050), CalculadoraDeSaldos.saldoAtual(corrente, ops, dia(30)))
        assertEquals(Money.ZERO, ResumoFinanceiro.doPeriodo(ops, dia(1), dia(30)).receitas)
    }

    // ---------- Livro: histórico e integridade ----------

    @Test
    fun registrarDuasVezesAMesmaOperacaoNaoDuplica() {
        val despesa = op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1)
        val livro = LivroFinanceiro.vazio().registrar(despesa)
        val deNovo = livro.registrar(despesa.copy(criadaEm = 999))
        assertSame(livro, deNovo)
        assertEquals(1, deNovo.operacoes.size)
    }

    @Test
    fun mesmoIdComDadosDiferentesEhRecusado() {
        val livro = LivroFinanceiro.vazio().registrar(op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1))
        assertThrows(RegraFinanceiraException::class.java) {
            livro.registrar(op("d1", TipoOperacao.DESPESA, 6, 300, conta = 1))
        }
    }

    @Test
    fun reverterPreservaOOriginalEAnulaOEfeito() {
        val livro = LivroFinanceiro.vazio()
            .registrar(op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1, categoria = mercado))
            .reverter("d1", "d1-estorno", dia(9), agora = 2)
        assertEquals(2, livro.operacoes.size)
        assertNotNull(livro.buscar("d1"))
        assertTrue(livro.foiRevertida("d1"))
        assertEquals(reais(800), CalculadoraDeSaldos.saldoAtual(corrente, livro.operacoes, dia(7)))
        assertEquals(reais(1000), CalculadoraDeSaldos.saldoAtual(corrente, livro.operacoes, dia(9)))
    }

    @Test
    fun naoPermiteEstornarDuasVezesNemEstornarEstorno() {
        val livro = LivroFinanceiro.vazio()
            .registrar(op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1))
            .reverter("d1", "e1", dia(9), agora = 2)
        assertThrows(RegraFinanceiraException::class.java) { livro.reverter("d1", "e2", dia(10)) }
        assertThrows(RegraFinanceiraException::class.java) { livro.reverter("e1", "e3", dia(10)) }
        assertThrows(RegraFinanceiraException::class.java) { livro.reverter("nao-existe", "e4", dia(10)) }
    }

    @Test
    fun estornoNaoPodeSerAnteriorAoOriginalNemDivergirDele() {
        val original = op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1)
        val livro = LivroFinanceiro.vazio().registrar(original)
        assertThrows(RegraFinanceiraException::class.java) { livro.reverter("d1", "e1", dia(5)) }
        assertThrows(RegraFinanceiraException::class.java) {
            livro.registrar(original.copy(id = "e1", valor = reais(150), reverteId = "d1"))
        }
    }

    @Test
    fun corrigirEstornaOAntigoERegistraONovo() {
        val livro = LivroFinanceiro.vazio()
            .registrar(op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1))
            .corrigir("d1", "d1-estorno", op("d1-v2", TipoOperacao.DESPESA, 6, 180, conta = 1), dia(9), agora = 2)
        assertEquals(3, livro.operacoes.size)
        assertEquals(reais(820), CalculadoraDeSaldos.saldoAtual(corrente, livro.operacoes, dia(30)))
    }

    @Test
    fun correcaoInvalidaNaoAlteraOLivro() {
        val livro = LivroFinanceiro.vazio().registrar(op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1))
        val invalida = op("d1-v2", TipoOperacao.TRANSFERENCIA, 6, 180, conta = 1, destino = 1)
        assertThrows(RegraFinanceiraException::class.java) {
            livro.corrigir("d1", "d1-estorno", invalida, dia(9))
        }
        assertEquals(1, livro.operacoes.size)
        assertFalse(livro.foiRevertida("d1"))
    }

    @Test
    fun previstaPodeSerConfirmadaOuCanceladaMasConfirmadaNao() {
        val livro = LivroFinanceiro.vazio()
            .registrar(op("p1", TipoOperacao.DESPESA, 15, 400, conta = 1, situacao = SituacaoOperacao.PREVISTA))
            .registrar(op("p2", TipoOperacao.DESPESA, 16, 100, conta = 1, situacao = SituacaoOperacao.PREVISTA))
            .confirmar("p1", dia(14))
            .cancelarPrevista("p2")
        assertEquals(SituacaoOperacao.CONFIRMADA, livro.buscar("p1")?.situacao)
        assertEquals(dia(14), livro.buscar("p1")?.data)
        assertEquals(SituacaoOperacao.CANCELADA, livro.buscar("p2")?.situacao)
        assertEquals(reais(600), CalculadoraDeSaldos.saldoAtual(corrente, livro.operacoes, dia(30)))
        assertThrows(RegraFinanceiraException::class.java) { livro.confirmar("p1", dia(20)) }
        assertThrows(RegraFinanceiraException::class.java) { livro.cancelarPrevista("p1") }
        assertThrows(RegraFinanceiraException::class.java) { livro.reverter("p2", "e1", dia(20)) }
    }

    @Test
    fun validacaoRecusaOperacoesMalFormadas() {
        val livro = LivroFinanceiro.vazio()
        val invalidas = listOf(
            op("z", TipoOperacao.DESPESA, 6, 0, conta = 1),
            op("n", TipoOperacao.DESPESA, 6, -5, conta = 1),
            op("s", TipoOperacao.DESPESA, 6, 10),
            op("t", TipoOperacao.TRANSFERENCIA, 6, 10, conta = 1),
            op("t2", TipoOperacao.TRANSFERENCIA, 6, 10, conta = 1, destino = 1),
            op("c", TipoOperacao.COMPRA_CARTAO, 6, 10, conta = 1, cartaoId = cartao),
            op("c2", TipoOperacao.COMPRA_CARTAO, 6, 10),
            op("f", TipoOperacao.PAGAMENTO_FATURA, 6, 10, conta = 1),
            op("", TipoOperacao.DESPESA, 6, 10, conta = 1)
        )
        invalidas.forEach { invalida ->
            assertThrows(RegraFinanceiraException::class.java) { livro.registrar(invalida) }
        }
    }

    @Test
    fun reconstruirOLivroValidaTodasAsOperacoes() {
        val ops = listOf(
            op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1),
            op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1)
        )
        assertEquals(1, LivroFinanceiro.de(ops).operacoes.size)
    }

    // ---------- Reconciliação ----------

    @Test
    fun reconciliacaoQueConfereNaoPropoeAjuste() {
        val ops = listOf(op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1))
        val resultado = Reconciliacao.conferir(corrente, ops, reais(800), dia(10), "aj1")
        assertTrue(resultado.confere)
        assertNull(resultado.ajusteProposto)
    }

    @Test
    fun reconciliacaoPropoeAjusteQueIgualaOSaldo() {
        val ops = listOf(op("d1", TipoOperacao.DESPESA, 6, 200, conta = 1))

        val faltando = Reconciliacao.conferir(corrente, ops, reais(770), dia(10), "aj1")
        assertEquals(reais(-30), faltando.diferenca)
        assertEquals(TipoOperacao.AJUSTE_DEBITO, faltando.ajusteProposto?.tipo)
        val comAjuste = LivroFinanceiro.de(ops).registrar(faltando.ajusteProposto!!)
        assertEquals(reais(770), CalculadoraDeSaldos.saldoAtual(corrente, comAjuste.operacoes, dia(10)))
        // O ajuste corrige o saldo sem inflar as despesas.
        assertEquals(reais(200), ResumoFinanceiro.doPeriodo(comAjuste.operacoes, dia(1), dia(30)).despesas)

        val sobrando = Reconciliacao.conferir(corrente, ops, reais(815), dia(10), "aj2")
        assertEquals(TipoOperacao.AJUSTE_CREDITO, sobrando.ajusteProposto?.tipo)
        assertEquals(reais(15), sobrando.ajusteProposto?.valor)
    }
}
