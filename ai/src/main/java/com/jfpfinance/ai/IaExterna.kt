package com.jfpfinance.ai

import com.jfpfinance.core.rotulo
import com.jfpfinance.engine.Analises
import com.jfpfinance.engine.DadosFinanceiros
import com.jfpfinance.engine.ResumoFinanceiro
import java.time.YearMonth

/**
 * O que seria enviado a uma IA externa em UMA pergunta (seção 19).
 *
 * [itensCompartilhados] e [finalidade] existem para serem mostrados ao usuário antes do envio;
 * [conteudo] é exatamente o texto que sairia do aparelho, para que ele possa conferir.
 */
data class PedidoExterno(
    val pergunta: String,
    val finalidade: String,
    val itensCompartilhados: List<String>,
    val conteudo: String
)

/**
 * Conexão com uma IA externa. Nenhuma implementação existe ainda: o provedor não foi escolhido.
 *
 * Regra obrigatória para qualquer implementação futura: só pode ser chamada depois de o usuário
 * ver o [PedidoExterno] e autorizar aquele envio específico. A autorização não é reaproveitada.
 */
interface IaExterna {
    suspend fun perguntar(pedido: PedidoExterno): String
}

/**
 * Monta o mínimo necessário para uma pergunta externa: só totais e percentuais.
 * Nunca inclui nomes de contas, credores, cartões, estabelecimentos, descrições de
 * lançamentos, datas de lançamentos individuais nem o banco de dados.
 */
object PreparadorDeEnvio {

    fun montar(pergunta: String, dados: DadosFinanceiros): PedidoExterno {
        val mes = YearMonth.from(dados.hoje)
        val resumo = ResumoFinanceiro.doPeriodo(dados.operacoes, mes.atDay(1), dados.hoje)
        val indicadores = Analises.indicadores(dados)
        val categorias = resumo.despesasPorCategoria.entries.sortedByDescending { it.value }.take(5)
            .joinToString("; ") { "${dados.nomeDaCategoria(it.key)}: ${it.value.formatarBRL()}" }

        val linhas = buildList {
            add("Mês de referência: ${mes.rotulo()} (parcial).")
            add("Receitas do mês: ${resumo.receitas.formatarBRL()}. Despesas do mês: ${resumo.despesas.formatarBRL()}.")
            if (categorias.isNotEmpty()) add("Maiores categorias de despesa: $categorias.")
            add("Saldo somado das contas: ${indicadores.saldoDasContas.formatarBRL()}.")
            add("Receita média mensal: ${indicadores.base.receitaMedia.formatarBRL()}. Despesa média mensal: ${indicadores.base.despesaMedia.formatarBRL()}.")
            add("Parcelas mensais de dívidas: ${indicadores.parcelasMensaisDeDividas.formatarBRL()}.")
        }
        return PedidoExterno(
            pergunta = pergunta.trim(),
            finalidade = "Obter uma explicação mais elaborada para a pergunta feita.",
            itensCompartilhados = listOf(
                "O texto da sua pergunta",
                "Totais de receitas e despesas do mês atual",
                "Totais das cinco maiores categorias de despesa (nome da categoria e valor)",
                "Saldo somado das contas, sem nomes de contas ou bancos",
                "Médias mensais de receita e despesa",
                "Soma das parcelas mensais de dívidas, sem nomes de credores"
            ),
            conteudo = linhas.joinToString("\n")
        )
    }
}
