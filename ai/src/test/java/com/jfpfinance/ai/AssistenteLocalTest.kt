package com.jfpfinance.ai

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.SistemaAmortizacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoCategoria
import com.jfpfinance.core.model.TipoConta
import com.jfpfinance.core.model.TipoMeta
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.engine.DadosFinanceiros
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistenteLocalTest {

    private val hoje = LocalDate.of(2026, 11, 18)
    private fun reais(valor: Long) = Money(valor * 100)
    private fun data(mes: Int, dia: Int) = LocalDate.of(2026, mes, dia)

    private val corrente = Conta(id = 1, nome = "Banco Azul", tipo = TipoConta.CONTA_BANCARIA, saldoInicial = reais(1000))
    private val alimentacao = Categoria(id = 100, nome = "Alimentação", tipo = TipoCategoria.DESPESA)
    private val lazer = Categoria(id = 101, nome = "Lazer", tipo = TipoCategoria.DESPESA)
    private val salario = Categoria(id = 200, nome = "Salário", tipo = TipoCategoria.RECEITA)

    private fun op(id: String, tipo: TipoOperacao, quando: LocalDate, valor: Long, categoria: Long? = null, situacao: SituacaoOperacao = SituacaoOperacao.CONFIRMADA, descricao: String = "") =
        Operacao(id = id, tipo = tipo, data = quando, valor = reais(valor), contaId = 1, categoriaId = categoria, situacao = situacao, descricao = descricao, criadaEm = 1)

    private val dados = DadosFinanceiros(
        hoje = hoje,
        contas = listOf(corrente),
        categorias = listOf(alimentacao, lazer, salario),
        cartoes = listOf(Cartao(id = 10, nome = "Cartão Ouro", limite = reais(5000), diaFechamento = 25, diaVencimento = 5)),
        metas = listOf(Meta(id = 1, nome = "Reserva", tipo = TipoMeta.RESERVA_EMERGENCIA, valorAlvo = reais(10000))),
        dividas = listOf(
            Divida(
                id = 1, credor = "Financeira Beta", valorOriginal = reais(12000), taxaMensalPercentual = BigDecimal("1"),
                parcelas = 12, sistema = SistemaAmortizacao.SAC, primeiraParcela = data(12, 10)
            )
        ),
        operacoes = listOf(
            op("r0", TipoOperacao.RECEITA, data(10, 5), 4000, salario.id),
            op("d0", TipoOperacao.DESPESA, data(10, 8), 1500, alimentacao.id),
            op("r1", TipoOperacao.RECEITA, data(11, 5), 4000, salario.id),
            op("d1", TipoOperacao.DESPESA, data(11, 6), 600, alimentacao.id, descricao = "Mercado da esquina"),
            op("d2", TipoOperacao.DESPESA, data(11, 8), 200, lazer.id),
            op("p1", TipoOperacao.DESPESA, data(11, 20), 350, situacao = SituacaoOperacao.PREVISTA, descricao = "Internet"),
            Operacao(id = "c1", tipo = TipoOperacao.COMPRA_CARTAO, data = data(11, 10), valor = reais(300), cartaoId = 10, categoriaId = lazer.id, criadaEm = 1)
        )
    )

    private fun perguntar(pergunta: String) = AssistenteLocal.responder(pergunta, dados)

    @Test
    fun respondeSaldoComPremissa() {
        val r = perguntar("Qual é o meu saldo?")
        // 1000 + 4000 - 1500 + 4000 - 600 - 200
        assertTrue(r.texto.contains("R$ 6.700,00"))
        assertTrue(r.texto.contains("Banco Azul"))
        assertTrue(r.premissas.single().contains("18/11/2026"))
    }

    @Test
    fun respondeGastosDoMesEDoMesPassado() {
        val atual = perguntar("quanto gastei este mês?")
        assertTrue(atual.texto.contains("novembro de 2026"))
        assertTrue(atual.texto.contains("R$ 1.100,00")) // 600 + 200 + 300 no cartão
        assertTrue(atual.premissas.first().contains("01/11/2026 a 18/11/2026"))

        val passado = perguntar("Quanto gastei no mês passado")
        assertTrue(passado.texto.contains("outubro de 2026"))
        assertTrue(passado.texto.contains("R$ 1.500,00"))
        assertTrue(passado.premissas.first().contains("31/10/2026"))
    }

    @Test
    fun entendePerguntasComOuSemAcentoEMaiusculas() {
        assertTrue(perguntar("QUAL O MEU PATRIMÔNIO").texto.contains("Patrimônio líquido"))
        assertTrue(perguntar("qual o meu patrimonio").texto.contains("Patrimônio líquido"))
        assertEquals("qual e o orcamento", AssistenteLocal.normalizar("Qual é o orçamento?!"))
    }

    @Test
    fun respondeCategoriasEmOrdemComPercentual() {
        val r = perguntar("Onde estou gastando mais?")
        val linhas = r.texto.lines()
        assertTrue(linhas[1].startsWith("Alimentação: R$ 600,00 (55%)"))
        assertTrue(linhas[2].startsWith("Lazer: R$ 500,00 (45%)"))
    }

    @Test
    fun respondeDemaisAssuntos() {
        assertTrue(perguntar("o que vence nos próximos dias").texto.contains("Internet: R$ 350,00 (a pagar)"))
        assertTrue(perguntar("como estão meus cartões").texto.contains("Cartão Ouro: em uso R$ 300,00"))
        val dividas = perguntar("quanto devo?")
        assertTrue(dividas.texto.contains("R$ 12.000,00"))
        assertTrue(dividas.texto.contains("Financeira Beta"))
        assertTrue(perguntar("minhas metas").texto.contains("Reserva: 0%"))
        assertTrue(perguntar("meus orçamentos").texto.contains("não definiu orçamentos"))
        assertTrue(perguntar("quanto recebi").texto.contains("Salário: R$ 4.000,00"))
        assertTrue(perguntar("quanto consigo poupar").texto.contains("R$ 2.500,00"))
        assertTrue(perguntar("como fica daqui a 6 meses").texto.contains("maio de 2027"))
        assertTrue(perguntar("como está minha saúde financeira").premissas.any { it.contains("não aconselhamento") })
    }

    @Test
    fun patrimonioSomaAtivosEPassivos() {
        // contas 6700 - dívida 12000 - cartão 300
        assertTrue(perguntar("patrimônio").texto.contains("Patrimônio líquido: -R$ 5.600,00"))
    }

    @Test
    fun perguntaForaDoEscopoNaoInventaResposta() {
        val r = perguntar("qual a previsão do tempo?")
        assertFalse(r.entendida)
        assertTrue(r.texto.contains("Não reconheci"))
        assertFalse(perguntar("   ").entendida)
    }

    @Test
    fun todasAsPerguntasDeExemploSaoEntendidas() {
        AssistenteLocal.EXEMPLOS.forEach { exemplo ->
            assertTrue("não entendeu: $exemplo", perguntar(exemplo).entendida)
        }
    }

    @Test
    fun envioExternoLevaSoTotaisSemNomesNemDescricoes() {
        val pedido = PreparadorDeEnvio.montar("  Como reduzir gastos?  ", dados)
        assertEquals("Como reduzir gastos?", pedido.pergunta)
        val tudo = pedido.conteudo
        assertTrue(tudo.contains("R$ 1.100,00"))
        assertTrue(tudo.contains("Alimentação"))
        listOf("Banco Azul", "Financeira Beta", "Cartão Ouro", "Mercado da esquina", "Internet", "Reserva").forEach { privado ->
            assertFalse("vazou: $privado", tudo.contains(privado))
        }
        assertTrue(pedido.itensCompartilhados.isNotEmpty())
        assertTrue(pedido.finalidade.isNotBlank())
    }
}
