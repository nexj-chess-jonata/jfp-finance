package com.jfpfinance.reports

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Pessoa
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoCategoria
import com.jfpfinance.core.model.TipoConta
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.engine.DadosFinanceiros
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatoriosTest {

    private fun reais(valor: Long) = Money(valor * 100)
    private fun data(dia: Int) = LocalDate.of(2026, 11, dia)

    private val dados = DadosFinanceiros(
        hoje = data(18),
        contas = listOf(
            Conta(id = 1, nome = "Corrente", tipo = TipoConta.CONTA_BANCARIA, saldoInicial = reais(1000)),
            Conta(id = 2, nome = "Poupança", tipo = TipoConta.CONTA_BANCARIA, saldoInicial = Money.ZERO)
        ),
        categorias = listOf(
            Categoria(id = 100, nome = "Alimentação", tipo = TipoCategoria.DESPESA),
            Categoria(id = 200, nome = "Salário", tipo = TipoCategoria.RECEITA)
        ),
        cartoes = listOf(Cartao(id = 10, nome = "Cartão", limite = reais(5000), diaFechamento = 25, diaVencimento = 5)),
        pessoas = listOf(Pessoa(id = 1, nome = "Ana"), Pessoa(id = 2, nome = "Beto")),
        operacoes = listOf(
            Operacao(id = "r1", tipo = TipoOperacao.RECEITA, data = data(5), valor = reais(4000), contaId = 1, categoriaId = 200, descricao = "Salário", criadaEm = 1),
            Operacao(id = "d1", tipo = TipoOperacao.DESPESA, data = data(6), valor = reais(600), contaId = 1, categoriaId = 100, descricao = "Mercado; \"da esquina\"", pessoaId = 1, criadaEm = 2),
            Operacao(id = "c1", tipo = TipoOperacao.COMPRA_CARTAO, data = data(7), valor = reais(200), cartaoId = 10, categoriaId = 100, descricao = "=SOMA(A1)", pessoaId = 2, criadaEm = 3),
            Operacao(id = "t1", tipo = TipoOperacao.TRANSFERENCIA, data = data(8), valor = reais(300), contaId = 1, contaDestinoId = 2, criadaEm = 4),
            Operacao(id = "p1", tipo = TipoOperacao.DESPESA, data = data(20), valor = reais(150), contaId = 1, situacao = SituacaoOperacao.PREVISTA, descricao = "Internet", criadaEm = 5),
            Operacao(id = "x1", tipo = TipoOperacao.DESPESA, data = data(9), valor = reais(50), contaId = 1, situacao = SituacaoOperacao.CANCELADA, criadaEm = 6),
            Operacao(id = "fora", tipo = TipoOperacao.DESPESA, data = LocalDate.of(2026, 10, 9), valor = reais(70), contaId = 1, criadaEm = 0)
        )
    )

    private val relatorio = Relatorios.doPeriodo(dados, data(1), data(30))
    private fun secao(titulo: String) = relatorio.secoes.first { it.titulo == titulo }
    private fun valor(titulo: String, rotulo: String) = secao(titulo).linhas.first { it.rotulo == rotulo }.valor

    @Test
    fun resumoSeparaRealizadoDePrevisto() {
        assertEquals(reais(4000), valor("Resumo do período", "Receitas"))
        assertEquals(reais(800), valor("Resumo do período", "Despesas"))
        assertEquals(reais(3200), valor("Resumo do período", "Resultado"))
        assertEquals(Natureza.REALIZADO, secao("Resumo do período").natureza)
        assertEquals(Natureza.PREVISTO, secao("Lançamentos previstos no período").natureza)
        assertEquals(reais(150), valor("Lançamentos previstos no período", "Despesas previstas"))
    }

    @Test
    fun secoesPorCategoriaPessoaEPosicao() {
        assertEquals(reais(800), valor("Despesas por categoria", "Alimentação"))
        assertEquals(reais(600), valor("Despesas por pessoa", "Ana"))
        assertEquals(reais(200), valor("Despesas por pessoa", "Beto"))
        // Corrente: 1000 + 4000 - 600 - 300 - 70 (outubro) = 4030; Poupança: 300
        assertEquals(reais(4030), valor("Saldo das contas", "Corrente"))
        assertEquals(reais(300), valor("Saldo das contas", "Poupança"))
        assertEquals(Natureza.POSICAO, secao("Patrimônio").natureza)
        assertEquals(reais(4130), valor("Patrimônio", "Patrimônio líquido"))
    }

    @Test
    fun lancamentosDoPeriodoSemCanceladosEComSinal() {
        assertEquals(listOf("Salário", "Mercado; \"da esquina\"", "=SOMA(A1)", "", "Internet"), relatorio.lancamentos.map { it.descricao })
        assertEquals(listOf(reais(4000), reais(-600), reais(-200), Money.ZERO, reais(-150)), relatorio.lancamentos.map { it.valor })
        assertEquals("Corrente → Poupança", relatorio.lancamentos[3].conta)
        assertEquals("Ana", relatorio.lancamentos[1].pessoa)
    }

    @Test
    fun filtroPorPessoaMostraSoOsLancamentosDela() {
        val daAna = Relatorios.doPeriodo(dados, data(1), data(30), pessoaId = 1, filtrarPorPessoa = true)
        assertEquals(listOf("Pessoa: Ana"), daAna.filtros)
        assertEquals(1, daAna.lancamentos.size)
        assertEquals(reais(600), daAna.secoes.first().linhas.first { it.rotulo == "Despesas" }.valor)
        assertFalse(daAna.secoes.any { it.natureza == Natureza.POSICAO })
    }

    @Test
    fun textoTrazPeriodoMoedaFiltrosEData() {
        val texto = Exportadores.texto(relatorio)
        assertEquals("Período: 01/11/2026 a 30/11/2026", texto[1])
        assertEquals("Moeda: BRL", texto[2])
        assertEquals("Filtros: Sem filtros", texto[3])
        assertEquals("Gerado em: 18/11/2026", texto[4])
        assertTrue(texto.any { it == "RESUMO DO PERÍODO — Realizado" })
        assertTrue(texto.any { it.contains("Internet") && it.endsWith("[previsto]") })
    }

    @Test
    fun csvEscapaSeparadorAspasEFormulas() {
        val csv = Exportadores.csv(relatorio)
        assertTrue(csv.startsWith("﻿"))
        assertTrue(csv.contains("06/11/2026;\"Mercado; \"\"da esquina\"\"\";Alimentação;Corrente;Ana;DESPESA;CONFIRMADA;-600,00\r\n"))
        // Texto que começa com "=" não pode virar fórmula no Excel.
        assertTrue(csv.contains(";'=SOMA(A1);"))
        assertFalse(csv.contains(";=SOMA"))
    }

    @Test
    fun jsonEscapaTextoEUsaCentavos() {
        val json = Exportadores.json(relatorio)
        assertTrue(json.contains("\"descricao\":\"Mercado; \\\"da esquina\\\"\""))
        assertTrue(json.contains("\"centavos\":-60000"))
        assertTrue(json.contains("\"inicio\":\"2026-11-01\""))
        // Estrutura balanceada.
        assertEquals(json.count { it == '{' }, json.count { it == '}' })
        assertEquals(json.count { it == '[' }, json.count { it == ']' })
    }
}
