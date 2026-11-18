package com.jfpfinance.ui.assistente

import com.jfpfinance.ai.AssistenteLocal
import com.jfpfinance.core.Money
import com.jfpfinance.engine.Analises
import com.jfpfinance.engine.Cenario
import com.jfpfinance.engine.DadosFinanceiros
import com.jfpfinance.engine.Diagnostico
import com.jfpfinance.engine.Projecao
import com.jfpfinance.engine.Projecoes
import java.math.BigDecimal

/*
 * Lógica de apresentação sem dependência de Android.
 * Diagnóstico, respostas e projeções vêm do motor e do assistente local.
 */

data class Mensagem(
    val doUsuario: Boolean,
    val texto: String,
    val premissas: List<String> = emptyList()
)

/** Prazos oferecidos na tela de projeções (seção 20). */
enum class Horizonte(val rotulo: String, val meses: Int) {
    DIAS_30("30 dias (1 mês)", 1),
    DIAS_90("90 dias (3 meses)", 3),
    MESES_6("6 meses", 6),
    MESES_12("12 meses", 12),
    MESES_24("24 meses", 24),
    MESES_60("60 meses", 60)
}

/** O que o usuário escolheu na tela de projeções. Nada disso é gravado. */
data class ConfiguracaoDaProjecao(
    val horizonte: Horizonte = Horizonte.MESES_6,
    val cenario: Cenario = Cenario.INTERMEDIARIO
)

data class AssistenteUiState(
    val carregando: Boolean = true,
    val dados: DadosFinanceiros? = null,
    val diagnostico: Diagnostico? = null,
    val conversa: List<Mensagem> = emptyList(),
    val configuracao: ConfiguracaoDaProjecao = ConfiguracaoDaProjecao(),
    val projecao: Projecao? = null
)

fun montarAssistente(
    dados: DadosFinanceiros,
    conversa: List<Mensagem>,
    configuracao: ConfiguracaoDaProjecao
): AssistenteUiState = AssistenteUiState(
    carregando = false,
    dados = dados,
    diagnostico = Analises.diagnostico(dados),
    conversa = conversa,
    configuracao = configuracao,
    projecao = Projecoes.projetar(dados, configuracao.cenario, configuracao.horizonte.meses)
)

/** Acrescenta a pergunta e a resposta do assistente local ao fim da conversa. */
fun conversaComResposta(conversa: List<Mensagem>, pergunta: String, dados: DadosFinanceiros): List<Mensagem> {
    val resposta = AssistenteLocal.responder(pergunta, dados)
    return conversa +
        Mensagem(doUsuario = true, texto = pergunta.trim()) +
        Mensagem(doUsuario = false, texto = resposta.texto, premissas = resposta.premissas)
}

const val NOME_DO_CENARIO_PERSONALIZADO = "Personalizado"

/** Lê um percentual como "10", "-7,5" ou vazio (= 0). Retorna null se não for número ou passar de ±100. */
fun lerPercentual(texto: String): BigDecimal? {
    if (texto.isBlank()) return BigDecimal.ZERO
    return try {
        BigDecimal(texto.trim().replace(',', '.')).takeIf { it.abs() <= BigDecimal.valueOf(100) }
    } catch (e: NumberFormatException) {
        null
    }
}

/** Lê um valor em reais que pode ser negativo; vazio = zero. */
fun lerAjuste(texto: String): Money? = if (texto.isBlank()) Money.ZERO else Money.deTexto(texto)

fun cenarioPersonalizado(
    variacaoDaReceita: BigDecimal,
    variacaoDaDespesa: BigDecimal,
    receitaExtraMensal: Money,
    despesaExtraMensal: Money
) = Cenario(
    nome = NOME_DO_CENARIO_PERSONALIZADO,
    variacaoDaReceita = variacaoDaReceita,
    variacaoDaDespesa = variacaoDaDespesa,
    receitaExtraMensal = receitaExtraMensal,
    despesaExtraMensal = despesaExtraMensal
)
