package com.jfpfinance.reports

import com.jfpfinance.core.Money
import com.jfpfinance.core.formatarBR
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.somar
import com.jfpfinance.engine.Amortizacao
import com.jfpfinance.engine.CalculadoraDeSaldos
import com.jfpfinance.engine.DadosFinanceiros
import com.jfpfinance.engine.Faturas
import com.jfpfinance.engine.Patrimonio
import com.jfpfinance.engine.ResumoFinanceiro
import com.jfpfinance.engine.efeitoEmDespesa
import com.jfpfinance.engine.efeitoEmReceita
import com.jfpfinance.engine.efeitoNaConta
import java.time.LocalDate

/** Natureza dos números de uma seção: a seção 25 exige que nunca se misturem. */
enum class Natureza(val rotulo: String) {
    REALIZADO("Realizado"),
    PREVISTO("Previsto (estimativa)"),
    POSICAO("Posição na data de geração")
}

data class LinhaDoRelatorio(val rotulo: String, val valor: Money)

data class SecaoDoRelatorio(val titulo: String, val natureza: Natureza, val linhas: List<LinhaDoRelatorio>)

data class LancamentoDoRelatorio(
    val data: LocalDate,
    val descricao: String,
    val categoria: String,
    val conta: String,
    val pessoa: String,
    val tipo: String,
    val situacao: String,
    /** Com sinal: positivo entra, negativo sai, do ponto de vista das contas e cartões do usuário. */
    val valor: Money
)

data class Relatorio(
    val titulo: String,
    val inicio: LocalDate,
    val fim: LocalDate,
    val moeda: String,
    val filtros: List<String>,
    val geradoEm: LocalDate,
    val secoes: List<SecaoDoRelatorio>,
    val lancamentos: List<LancamentoDoRelatorio>
)

object Relatorios {

    /**
     * Relatório de um período (seção 25). Com [pessoaId], considera só os lançamentos dessa pessoa
     * e deixa de fora as seções de posição (saldos, patrimônio), que são da casa toda.
     */
    fun doPeriodo(
        dados: DadosFinanceiros,
        inicio: LocalDate,
        fim: LocalDate,
        pessoaId: Long? = null,
        filtrarPorPessoa: Boolean = false
    ): Relatorio {
        require(!fim.isBefore(inicio)) { "O fim do período não pode ser anterior ao início." }
        val operacoes = if (filtrarPorPessoa) dados.operacoes.filter { it.pessoaId == pessoaId } else dados.operacoes
        val realizado = ResumoFinanceiro.doPeriodo(operacoes, inicio, fim)
        val previsto = ResumoFinanceiro.doPeriodo(operacoes, inicio, fim, SituacaoOperacao.PREVISTA)

        fun porCategoria(mapa: Map<Long?, Money>) = mapa.entries.sortedByDescending { it.value }
            .map { LinhaDoRelatorio(dados.nomeDaCategoria(it.key), it.value) }

        val secoes = buildList {
            add(
                SecaoDoRelatorio(
                    "Resumo do período", Natureza.REALIZADO,
                    listOf(
                        LinhaDoRelatorio("Receitas", realizado.receitas),
                        LinhaDoRelatorio("Despesas", realizado.despesas),
                        LinhaDoRelatorio("Resultado", realizado.resultado)
                    )
                )
            )
            if (realizado.despesasPorCategoria.isNotEmpty()) {
                add(SecaoDoRelatorio("Despesas por categoria", Natureza.REALIZADO, porCategoria(realizado.despesasPorCategoria)))
            }
            if (realizado.receitasPorCategoria.isNotEmpty()) {
                add(SecaoDoRelatorio("Receitas por categoria", Natureza.REALIZADO, porCategoria(realizado.receitasPorCategoria)))
            }
            if (!filtrarPorPessoa && dados.pessoas.isNotEmpty()) {
                val noPeriodo = operacoes.filter {
                    it.situacao == SituacaoOperacao.CONFIRMADA && !it.data.isBefore(inicio) && !it.data.isAfter(fim)
                }
                val porPessoa = noPeriodo.groupBy { it.pessoaId }
                    .mapValues { (_, lista) -> lista.map { it.efeitoEmDespesa() }.somar() }
                    .filterValues { !it.isZero }
                    .entries.sortedByDescending { it.value }
                    .map { LinhaDoRelatorio(dados.nomeDaPessoa(it.key), it.value) }
                if (porPessoa.isNotEmpty()) add(SecaoDoRelatorio("Despesas por pessoa", Natureza.REALIZADO, porPessoa))
            }
            if (!previsto.receitas.isZero || !previsto.despesas.isZero) {
                add(
                    SecaoDoRelatorio(
                        "Lançamentos previstos no período", Natureza.PREVISTO,
                        listOf(
                            LinhaDoRelatorio("Receitas previstas", previsto.receitas),
                            LinhaDoRelatorio("Despesas previstas", previsto.despesas)
                        )
                    )
                )
            }
            if (!filtrarPorPessoa) addAll(secoesDePosicao(dados))
        }

        val lancamentos = operacoes
            .filter { it.situacao != SituacaoOperacao.CANCELADA && !it.data.isBefore(inicio) && !it.data.isAfter(fim) }
            .sortedWith(compareBy<Operacao> { it.data }.thenBy { it.criadaEm })
            .map { linha(it, dados) }

        return Relatorio(
            titulo = "Relatório financeiro",
            inicio = inicio,
            fim = fim,
            moeda = "BRL",
            filtros = if (filtrarPorPessoa) listOf("Pessoa: ${dados.nomeDaPessoa(pessoaId)}") else listOf("Sem filtros"),
            geradoEm = dados.hoje,
            secoes = secoes,
            lancamentos = lancamentos
        )
    }

    private fun secoesDePosicao(dados: DadosFinanceiros): List<SecaoDoRelatorio> = buildList {
        val saldos = dados.contas.map { it to CalculadoraDeSaldos.saldoAtual(it, dados.operacoes, dados.hoje) }
        if (saldos.isNotEmpty()) {
            add(SecaoDoRelatorio("Saldo das contas", Natureza.POSICAO, saldos.map { LinhaDoRelatorio(it.first.nome, it.second) }))
        }
        val dividas = dados.dividas.map { LinhaDoRelatorio(it.credor, Amortizacao.resumo(it).saldoDevedor) }
        if (dividas.isNotEmpty()) add(SecaoDoRelatorio("Saldo devedor das dívidas", Natureza.POSICAO, dividas))
        val investimentos = dados.investimentos.map { LinhaDoRelatorio(it.nome, it.valorAtual) }
        if (investimentos.isNotEmpty()) {
            add(SecaoDoRelatorio("Investimentos (valor informado)", Natureza.POSICAO, investimentos))
        }
        val pl = Patrimonio.calcular(
            saldosDasContas = saldos.map { it.second },
            bens = dados.bens,
            investimentos = dados.investimentos,
            saldosDevedoresDasDividas = dados.dividas.map { Amortizacao.resumo(it).saldoDevedor },
            dividasDosCartoes = dados.cartoes.map { Faturas.limiteUtilizado(it, dados.operacoes) }
        )
        add(
            SecaoDoRelatorio(
                "Patrimônio", Natureza.POSICAO,
                listOf(
                    LinhaDoRelatorio("Patrimônio bruto", pl.bruto),
                    LinhaDoRelatorio("Passivos", pl.passivos),
                    LinhaDoRelatorio("Patrimônio líquido", pl.liquido)
                )
            )
        )
    }

    private fun linha(operacao: Operacao, dados: DadosFinanceiros): LancamentoDoRelatorio {
        val conta = dados.contas.firstOrNull { it.id == operacao.contaId }?.nome
        val destino = dados.contas.firstOrNull { it.id == operacao.contaDestinoId }?.nome
        val cartao = dados.cartoes.firstOrNull { it.id == operacao.cartaoId }?.nome
        val nasContas = dados.contas.map { operacao.efeitoNaConta(it.id) }.somar()
        val valor = when {
            !nasContas.isZero -> nasContas
            !operacao.efeitoEmDespesa().isZero -> -operacao.efeitoEmDespesa()
            !operacao.efeitoEmReceita().isZero -> operacao.efeitoEmReceita()
            else -> Money.ZERO
        }
        return LancamentoDoRelatorio(
            data = operacao.data,
            descricao = operacao.descricao,
            categoria = if (operacao.categoriaId == null) "" else dados.nomeDaCategoria(operacao.categoriaId),
            conta = listOfNotNull(conta, destino?.let { "→ $it" }, cartao).joinToString(" "),
            pessoa = if (operacao.pessoaId == null) "" else dados.nomeDaPessoa(operacao.pessoaId),
            tipo = operacao.tipo.name + if (operacao.ehReversao) " (estorno)" else "",
            situacao = operacao.situacao.name,
            valor = valor
        )
    }
}

/** Saídas do relatório. Todas informam período, moeda, filtros e data de geração. */
object Exportadores {

    private fun cabecalho(r: Relatorio): List<String> = listOf(
        r.titulo,
        "Período: ${r.inicio.formatarBR()} a ${r.fim.formatarBR()}",
        "Moeda: ${r.moeda}",
        "Filtros: ${r.filtros.joinToString("; ")}",
        "Gerado em: ${r.geradoEm.formatarBR()}"
    )

    /** Texto simples, usado na prévia e no PDF. */
    fun texto(r: Relatorio): List<String> = buildList {
        addAll(cabecalho(r))
        r.secoes.forEach { secao ->
            add("")
            add("${secao.titulo.uppercase()} — ${secao.natureza.rotulo}")
            secao.linhas.forEach { add("${it.rotulo}: ${it.valor.formatarBRL()}") }
        }
        add("")
        add("LANÇAMENTOS DO PERÍODO (${r.lancamentos.size})")
        r.lancamentos.forEach { l ->
            val detalhes = listOf(l.descricao, l.categoria, l.conta, l.pessoa).filter { it.isNotBlank() }.joinToString(" · ")
            val marca = if (l.situacao == SituacaoOperacao.PREVISTA.name) " [previsto]" else ""
            add("${l.data.formatarBR()}  ${l.valor.formatarBRL()}  $detalhes$marca")
        }
    }

    /**
     * CSV dos lançamentos para abrir no Excel: separador ponto e vírgula, decimal com vírgula.
     * Células que começam com = + - @ recebem um apóstrofo, para o Excel não as executar como fórmula.
     */
    fun csv(r: Relatorio): String {
        val linhas = mutableListOf<List<String>>()
        cabecalho(r).forEach { linhas.add(listOf(it)) }
        linhas.add(emptyList())
        linhas.add(listOf("Data", "Descrição", "Categoria", "Conta", "Pessoa", "Tipo", "Situação", "Valor"))
        r.lancamentos.forEach { l ->
            linhas.add(
                listOf(
                    l.data.formatarBR(), seguro(l.descricao), seguro(l.categoria), seguro(l.conta), seguro(l.pessoa),
                    l.tipo, l.situacao, l.valor.toBigDecimal().toPlainString().replace('.', ',')
                )
            )
        }
        // BOM para o Excel reconhecer os acentos.
        return "﻿" + linhas.joinToString("\r\n") { celulas -> celulas.joinToString(";", transform = ::celulaCsv) } + "\r\n"
    }

    private fun seguro(texto: String): String =
        if (texto.isNotEmpty() && texto[0] in "=+-@\t\r") "'$texto" else texto

    private fun celulaCsv(texto: String): String =
        if (texto.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + texto.replace("\"", "\"\"") + "\"" else texto

    /** JSON com valores em centavos (inteiros), para não perder precisão. */
    fun json(r: Relatorio): String {
        fun s(texto: String): String = buildString {
            append('"')
            texto.forEach { c ->
                when {
                    c == '"' -> append("\\\"")
                    c == '\\' -> append("\\\\")
                    c == '\n' -> append("\\n")
                    c == '\r' -> append("\\r")
                    c == '\t' -> append("\\t")
                    c < ' ' -> append("\\u%04x".format(c.code))
                    else -> append(c)
                }
            }
            append('"')
        }
        val secoes = r.secoes.joinToString(",") { secao ->
            val linhas = secao.linhas.joinToString(",") { "{\"rotulo\":${s(it.rotulo)},\"centavos\":${it.valor.centavos}}" }
            "{\"titulo\":${s(secao.titulo)},\"natureza\":${s(secao.natureza.name)},\"linhas\":[$linhas]}"
        }
        val lancamentos = r.lancamentos.joinToString(",") { l ->
            "{\"data\":${s(l.data.toString())},\"descricao\":${s(l.descricao)},\"categoria\":${s(l.categoria)}," +
                "\"conta\":${s(l.conta)},\"pessoa\":${s(l.pessoa)},\"tipo\":${s(l.tipo)}," +
                "\"situacao\":${s(l.situacao)},\"centavos\":${l.valor.centavos}}"
        }
        return "{\"titulo\":${s(r.titulo)},\"inicio\":${s(r.inicio.toString())},\"fim\":${s(r.fim.toString())}," +
            "\"moeda\":${s(r.moeda)},\"filtros\":[${r.filtros.joinToString(",") { s(it) }}]," +
            "\"geradoEm\":${s(r.geradoEm.toString())},\"secoes\":[$secoes],\"lancamentos\":[$lancamentos]}"
    }
}
