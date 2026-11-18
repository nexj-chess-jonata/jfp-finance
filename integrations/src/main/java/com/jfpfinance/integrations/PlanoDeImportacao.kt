package com.jfpfinance.integrations

import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.OrigemOperacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoCategoria
import com.jfpfinance.core.model.TipoOperacao
import java.security.MessageDigest

enum class SituacaoDoItem(val rotulo: String) {
    NOVO("Novo"),
    JA_IMPORTADO("Já importado antes"),
    POSSIVEL_DUPLICADO("Parecido com um lançamento que já existe")
}

/**
 * Uma linha da prévia de importação. [operacao] é a proposta: só vira registro
 * se o usuário confirmar e o motor aceitar.
 */
data class ItemDaImportacao(
    val lancamento: LancamentoImportado,
    val operacao: Operacao,
    val situacao: SituacaoDoItem,
    /** Vem marcado só o que é novo; duplicados precisam ser marcados à mão. */
    val selecionado: Boolean
)

object PlanoDeImportacao {

    /**
     * Monta a prévia (seção 24): identifica a conta, sugere categoria, detecta duplicidade.
     *
     * Cada lançamento recebe um identificador calculado a partir do seu conteúdo. Importar o
     * mesmo arquivo de novo gera os mesmos identificadores, e o motor não grava duas vezes.
     */
    fun montar(
        lidos: List<LancamentoImportado>,
        contaId: Long,
        existentes: List<Operacao>,
        categorias: List<Categoria>,
        nomeDoArquivo: String,
        agora: Long,
        moeda: String = "BRL"
    ): List<ItemDaImportacao> {
        val idsExistentes = existentes.map { it.id }.toSet()
        val ativos = existentes.filter { it.situacao != SituacaoOperacao.CANCELADA && !it.ehReversao }
        val sugestoes = sugestoesDeCategoria(existentes, categorias)
        val repeticoes = HashMap<String, Int>()

        return lidos.map { lido ->
            val tipo = if (lido.valor.isPositivo) TipoOperacao.RECEITA else TipoOperacao.DESPESA
            val valor = if (lido.valor.isPositivo) lido.valor else -lido.valor
            // Duas linhas idênticas no mesmo arquivo (dois cafés iguais no mesmo dia) são lançamentos diferentes.
            val base = lido.idExterno?.let { "fitid|$contaId|$it" }
                ?: "linha|$contaId|${lido.data}|${lido.valor.centavos}|${chave(lido.descricao)}"
            val ocorrencia = repeticoes.merge(base, 1, Int::plus) ?: 1
            val id = "imp-" + resumo("$base|$ocorrencia")

            val situacao = when {
                id in idsExistentes -> SituacaoDoItem.JA_IMPORTADO
                ativos.any { it.contaId == contaId && it.data == lido.data && it.valor == valor && it.tipo == tipo } ->
                    SituacaoDoItem.POSSIVEL_DUPLICADO
                else -> SituacaoDoItem.NOVO
            }
            val tipoDeCategoria = if (tipo == TipoOperacao.RECEITA) TipoCategoria.RECEITA else TipoCategoria.DESPESA
            ItemDaImportacao(
                lancamento = lido,
                operacao = Operacao(
                    id = id,
                    tipo = tipo,
                    data = lido.data,
                    valor = valor,
                    moeda = moeda,
                    contaId = contaId,
                    categoriaId = sugestoes[chave(lido.descricao) to tipoDeCategoria],
                    descricao = lido.descricao.take(120),
                    origem = OrigemOperacao.IMPORTACAO,
                    origemDetalhe = nomeDoArquivo,
                    criadaEm = agora
                ),
                situacao = situacao,
                selecionado = situacao == SituacaoDoItem.NOVO
            )
        }
    }

    /** Categoria mais usada em lançamentos anteriores com a mesma descrição. */
    private fun sugestoesDeCategoria(
        existentes: List<Operacao>,
        categorias: List<Categoria>
    ): Map<Pair<String, TipoCategoria>, Long> {
        val tipos = categorias.associate { it.id to it.tipo }
        return existentes
            .filter { it.categoriaId != null && it.descricao.isNotBlank() && tipos[it.categoriaId] != null }
            .groupBy { chave(it.descricao) to tipos.getValue(it.categoriaId!!) }
            .mapValues { (_, operacoes) ->
                operacoes.groupingBy { it.categoriaId!! }.eachCount().maxByOrNull { it.value }!!.key
            }
    }

    private fun chave(descricao: String): String = descricao.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun resumo(texto: String): String =
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
            .take(16).joinToString("") { "%02x".format(it) }
}
