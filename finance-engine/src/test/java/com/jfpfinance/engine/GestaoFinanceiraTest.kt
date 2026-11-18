package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoConta
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.core.repository.OperacaoRepository
import com.jfpfinance.core.somar
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GestaoFinanceiraTest {

    private val corrente = Conta(id = 1, nome = "Corrente", tipo = TipoConta.CONTA_BANCARIA, saldoInicial = reais(1000))
    private val cartao = Cartao(id = 10, nome = "Cartão", limite = reais(5000), diaFechamento = 25, diaVencimento = 5)

    private fun reais(valor: Long) = Money(valor * 100)
    private fun data(mes: Int, dia: Int) = LocalDate.of(2026, mes, dia)

    private fun compra(id: String, quando: LocalDate, valor: Long) = Operacao(
        id = id, tipo = TipoOperacao.COMPRA_CARTAO, data = quando, valor = reais(valor),
        cartaoId = cartao.id, criadaEm = 1
    )

    private fun despesa(id: String, quando: LocalDate, valor: Long, situacao: SituacaoOperacao = SituacaoOperacao.CONFIRMADA) =
        Operacao(id = id, tipo = TipoOperacao.DESPESA, data = quando, valor = reais(valor), contaId = 1, situacao = situacao, criadaEm = 1)

    private class RepositorioEmMemoria : OperacaoRepository {
        val dados = LinkedHashMap<String, Operacao>()
        var gravacoes = 0
        private val fluxo = MutableStateFlow<List<Operacao>>(emptyList())
        override fun observarTodas(): Flow<List<Operacao>> = fluxo
        override suspend fun listarTodas(): List<Operacao> = dados.values.toList()
        override suspend fun gravar(operacoes: List<Operacao>) {
            gravacoes++
            operacoes.forEach { dados[it.id] = it }
            fluxo.value = dados.values.toList()
        }
    }

    private fun contador(): () -> String {
        var n = 0
        return { "id-${++n}" }
    }

    // ---------- Ciclo de fatura ----------

    @Test
    fun compraAntesDoFechamentoEntraNaFaturaDoMes() {
        assertEquals(data(11, 25), Faturas.fechamentoDaCompra(cartao, data(11, 24)))
        assertEquals(data(12, 25), Faturas.fechamentoDaCompra(cartao, data(11, 25)))
        assertEquals(data(12, 25), Faturas.fechamentoDaCompra(cartao, data(11, 26)))
        assertEquals(data(12, 5), Faturas.vencimentoDaFatura(cartao, data(11, 25)))
    }

    @Test
    fun diasInexistentesUsamOUltimoDiaDoMes() {
        val dia31 = Cartao(id = 11, nome = "X", limite = reais(100), diaFechamento = 31, diaVencimento = 10)
        assertEquals(data(2, 28), Faturas.fechamentoDaCompra(dia31, data(2, 10)))
        assertEquals(data(3, 31), Faturas.fechamentoDaCompra(dia31, data(2, 28)))
        assertEquals(data(3, 10), Faturas.vencimentoDaFatura(dia31, data(2, 28)))
        // Vencimento antes do fechamento no mesmo mês: vence no mês seguinte.
        val vencimentoDepois = Cartao(id = 12, nome = "Y", limite = reais(100), diaFechamento = 3, diaVencimento = 10)
        assertEquals(data(11, 10), Faturas.vencimentoDaFatura(vencimentoDepois, data(11, 3)))
    }

    @Test
    fun pagamentosAbatemDaFaturaMaisAntiga() {
        val ops = listOf(
            compra("c1", data(10, 10), 300),
            compra("c2", data(11, 10), 200),
            Operacao(
                id = "p1", tipo = TipoOperacao.PAGAMENTO_FATURA, data = data(11, 5), valor = reais(350),
                contaId = 1, cartaoId = cartao.id, criadaEm = 2
            )
        )
        val faturas = Faturas.listar(cartao, ops)
        assertEquals(2, faturas.size)
        assertEquals(reais(300), faturas[0].total)
        assertTrue(faturas[0].quitada)
        assertEquals(reais(200), faturas[1].total)
        assertEquals(reais(50), faturas[1].pago)
        assertEquals(reais(150), faturas[1].restante)
        assertEquals(reais(150), Faturas.limiteUtilizado(cartao, ops))
        assertEquals(reais(4850), Faturas.limiteDisponivel(cartao, ops))
    }

    // ---------- Parcelas e repetições ----------

    @Test
    fun parcelasSomamOTotalECaemEmFaturasSeguidas() {
        val parcelas = Geradores.parcelar(compra("c1", data(11, 10), 100), 3, contador())
        assertEquals(listOf(Money(3334), Money(3333), Money(3333)), parcelas.map { it.valor })
        assertEquals(reais(100), parcelas.map { it.valor }.somar())
        assertEquals(listOf(data(11, 10), data(12, 10), LocalDate.of(2027, 1, 10)), parcelas.map { it.data })
        assertEquals(listOf(1, 2, 3), parcelas.map { it.parcela })
        assertEquals(1, parcelas.map { it.grupoId }.distinct().size)
        assertTrue(parcelas.all { it.situacao == SituacaoOperacao.CONFIRMADA })

        val livro = LivroFinanceiro.de(parcelas)
        assertEquals(3, Faturas.listar(cartao, livro.operacoes).size)
        // O limite é consumido pelo total desde a compra.
        assertEquals(reais(100), Faturas.limiteUtilizado(cartao, livro.operacoes))
        // A despesa aparece mês a mês.
        assertEquals(Money(3334), ResumoFinanceiro.doPeriodo(livro.operacoes, data(11, 1), data(11, 30)).despesas)
        assertEquals(Money(3333), ResumoFinanceiro.doPeriodo(livro.operacoes, data(12, 1), data(12, 31)).despesas)
    }

    @Test
    fun parcelaDeDia31RespeitaMesesCurtos() {
        val parcelas = Geradores.parcelar(compra("c1", data(1, 31), 90), 3, contador())
        assertEquals(listOf(data(1, 31), data(2, 28), data(3, 31)), parcelas.map { it.data })
    }

    @Test
    fun despesaParceladaForaDoCartaoFicaPrevistaNasProximas() {
        val parcelas = Geradores.parcelar(despesa("d1", data(11, 10), 300), 3, contador())
        assertEquals(
            listOf(SituacaoOperacao.CONFIRMADA, SituacaoOperacao.PREVISTA, SituacaoOperacao.PREVISTA),
            parcelas.map { it.situacao }
        )
    }

    @Test
    fun repeticaoMensalMantemValorEMarcaFuturasComoPrevistas() {
        val serie = Geradores.repetirMensalmente(despesa("d1", data(11, 10), 120), 3, contador())
        assertEquals(3, serie.size)
        assertTrue(serie.all { it.valor == reais(120) })
        assertEquals(SituacaoOperacao.CONFIRMADA, serie[0].situacao)
        assertTrue(serie.drop(1).all { it.situacao == SituacaoOperacao.PREVISTA })
        assertEquals(1, Geradores.repetirMensalmente(despesa("d2", data(11, 10), 120), 1, contador()).size)
    }

    @Test
    fun geradoresRecusamPedidosInvalidos() {
        assertThrows(RegraFinanceiraException::class.java) {
            Geradores.parcelar(compra("c1", data(11, 10), 100), 0, contador())
        }
        val receita = despesa("r1", data(11, 10), 100).copy(tipo = TipoOperacao.RECEITA)
        assertThrows(RegraFinanceiraException::class.java) { Geradores.parcelar(receita, 2, contador()) }
    }

    // ---------- Contas a pagar e a receber ----------

    @Test
    fun separaVencidosEProximos() {
        val ops = listOf(
            despesa("v1", data(11, 5), 100, SituacaoOperacao.PREVISTA),
            despesa("p1", data(11, 12), 100, SituacaoOperacao.PREVISTA),
            despesa("longe", data(12, 20), 100, SituacaoOperacao.PREVISTA),
            despesa("paga", data(11, 6), 100)
        )
        val pendentes = ContasAPagarEReceber.pendentes(ops, hoje = data(11, 10))
        assertEquals(listOf("v1"), pendentes.vencidos.map { it.id })
        assertEquals(listOf("p1"), pendentes.proximos.map { it.id })
    }

    @Test
    fun apontaOPrimeiroDiaDeSaldoNegativo() {
        val ops = listOf(
            despesa("p1", data(11, 12), 600, SituacaoOperacao.PREVISTA),
            despesa("p2", data(11, 20), 600, SituacaoOperacao.PREVISTA)
        )
        assertEquals(data(11, 20), ContasAPagarEReceber.primeiroDiaNegativo(corrente, ops, data(11, 10), data(11, 30)))
        assertNull(ContasAPagarEReceber.primeiroDiaNegativo(corrente, ops, data(11, 10), data(11, 15)))
        // Vencido e pendente conta como devido hoje.
        val vencido = listOf(despesa("v1", data(11, 1), 1500, SituacaoOperacao.PREVISTA))
        assertEquals(data(11, 10), ContasAPagarEReceber.primeiroDiaNegativo(corrente, vencido, data(11, 10), data(11, 30)))
    }

    // ---------- Gravação pelo motor ----------

    @Test
    fun gravaSomenteOQueMudouENadaQuandoARegraFalha() = runBlocking {
        val repositorio = RepositorioEmMemoria()
        val gestao = GestaoFinanceira(repositorio, contador(), agora = { 5 })

        gestao.registrar(despesa("d1", data(11, 6), 200))
        assertEquals(1, repositorio.dados.size)

        // Repetir o mesmo registro não grava de novo.
        gestao.registrar(despesa("d1", data(11, 6), 200))
        assertEquals(1, repositorio.gravacoes)

        // Lote com uma operação inválida: nenhuma é gravada.
        val invalida = despesa("d3", data(11, 6), 0)
        try {
            gestao.registrar(listOf(despesa("d2", data(11, 7), 50), invalida))
            throw AssertionError("deveria ter falhado")
        } catch (esperado: RegraFinanceiraException) {
            // ok
        }
        assertEquals(1, repositorio.dados.size)

        gestao.estornar("d1", data(11, 9))
        assertEquals(2, repositorio.dados.size)
        assertEquals(reais(1000), CalculadoraDeSaldos.saldoAtual(corrente, repositorio.listarTodas(), data(11, 30)))
    }

    @Test
    fun fluxoCompletoDeCartaoPeloServico() = runBlocking {
        val repositorio = RepositorioEmMemoria()
        val gestao = GestaoFinanceira(repositorio, contador(), agora = { 5 })

        gestao.registrarParcelado(compra("c1", data(11, 10), 300), 3)
        gestao.pagarFatura(cartao.id, contaId = 1, valor = reais(100), data = data(12, 5))

        val ops = repositorio.listarTodas()
        assertEquals(4, ops.size)
        val faturas = Faturas.listar(cartao, ops)
        assertTrue(faturas[0].quitada)
        assertEquals(reais(200), Faturas.limiteUtilizado(cartao, ops))
        assertEquals(reais(900), CalculadoraDeSaldos.saldoAtual(corrente, ops, data(12, 31)))
    }

    @Test
    fun previstaConfirmadaPeloServicoAlteraOSaldo() = runBlocking {
        val repositorio = RepositorioEmMemoria()
        val gestao = GestaoFinanceira(repositorio, contador(), agora = { 5 })
        gestao.registrarRepetindo(despesa("d1", data(11, 10), 100, SituacaoOperacao.PREVISTA), 2)
        assertEquals(reais(1000), CalculadoraDeSaldos.saldoAtual(corrente, repositorio.listarTodas(), data(12, 31)))

        gestao.confirmar("d1", data(11, 11))
        gestao.cancelarPrevista("id-2")
        assertEquals(reais(900), CalculadoraDeSaldos.saldoAtual(corrente, repositorio.listarTodas(), data(12, 31)))
        assertEquals(SituacaoOperacao.CANCELADA, repositorio.dados["id-2"]?.situacao)
    }
}
