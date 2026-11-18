package com.jfpfinance.app.ui.dados

import com.jfpfinance.core.model.Operacao
import com.jfpfinance.engine.DadosFinanceiros
import com.jfpfinance.integrations.ItemDaImportacao
import com.jfpfinance.integrations.LancamentoImportado
import com.jfpfinance.integrations.PlanoDeImportacao
import com.jfpfinance.reports.Relatorio

/*
 * Lógica de apresentação sem dependência de Android.
 * A prévia de importação é só uma proposta em memória: nada é gravado até o usuário confirmar.
 */

data class ImportacaoEmAndamento(
    val nomeDoArquivo: String,
    val lidos: List<LancamentoImportado>,
    val avisos: List<String>,
    val contaId: Long,
    val itens: List<ItemDaImportacao>
) {
    val selecionados: List<Operacao> get() = itens.filter { it.selecionado }.map { it.operacao }
}

data class DadosUiState(
    val dados: DadosFinanceiros? = null,
    val importacao: ImportacaoEmAndamento? = null,
    val relatorio: Relatorio? = null,
    val ocupado: Boolean = false,
    val mensagem: String? = null
)

fun iniciarImportacao(
    nomeDoArquivo: String,
    lidos: List<LancamentoImportado>,
    avisos: List<String>,
    contaId: Long,
    dados: DadosFinanceiros,
    agora: Long
) = ImportacaoEmAndamento(
    nomeDoArquivo = nomeDoArquivo,
    lidos = lidos,
    avisos = avisos,
    contaId = contaId,
    itens = PlanoDeImportacao.montar(lidos, contaId, dados.operacoes, dados.categorias, nomeDoArquivo, agora)
)

/** Trocar a conta refaz a prévia: a detecção de duplicados depende da conta. */
fun ImportacaoEmAndamento.comConta(contaId: Long, dados: DadosFinanceiros, agora: Long) =
    iniciarImportacao(nomeDoArquivo, lidos, avisos, contaId, dados, agora)

fun ImportacaoEmAndamento.alternando(indice: Int) = copy(
    itens = itens.mapIndexed { i, item -> if (i == indice) item.copy(selecionado = !item.selecionado) else item }
)

fun ImportacaoEmAndamento.comCategoria(indice: Int, categoriaId: Long?) = copy(
    itens = itens.mapIndexed { i, item ->
        if (i == indice) item.copy(operacao = item.operacao.copy(categoriaId = categoriaId)) else item
    }
)
