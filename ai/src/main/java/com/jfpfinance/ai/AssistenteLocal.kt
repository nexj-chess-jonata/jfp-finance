package com.jfpfinance.ai

import com.jfpfinance.core.formatarBR
import com.jfpfinance.core.rotulo
import com.jfpfinance.core.somar
import com.jfpfinance.engine.Amortizacao
import com.jfpfinance.engine.Analises
import com.jfpfinance.engine.CalculadoraDeSaldos
import com.jfpfinance.engine.Cenario
import com.jfpfinance.engine.ContasAPagarEReceber
import com.jfpfinance.engine.DadosFinanceiros
import com.jfpfinance.engine.Faturas
import com.jfpfinance.engine.Gravidade
import com.jfpfinance.engine.Metas
import com.jfpfinance.engine.Orcamentos
import com.jfpfinance.engine.Patrimonio
import com.jfpfinance.engine.Projecoes
import com.jfpfinance.engine.ResumoFinanceiro
import com.jfpfinance.engine.SituacaoDoOrcamento
import com.jfpfinance.engine.efeitoNaConta
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth

/**
 * Resposta do assistente. [premissas] diz de onde vieram os números e o que ficou de fora.
 * [entendida] = false quando a pergunta não corresponde a nenhum assunto conhecido.
 */
data class RespostaDoAssistente(
    val texto: String,
    val premissas: List<String> = emptyList(),
    val entendida: Boolean = true
)

/**
 * Assistente local (seção 19): funciona sem internet e não usa modelo de linguagem.
 * Reconhece o assunto da pergunta por palavras-chave e responde com os números do
 * motor financeiro. Só lê dados: não cria, altera nem apaga nada.
 */
object AssistenteLocal {

    val EXEMPLOS = listOf(
        "Qual é o meu saldo?",
        "Quanto gastei este mês?",
        "Onde estou gastando mais?",
        "O que vence nos próximos dias?",
        "Como estão meus cartões?",
        "Quanto devo?",
        "Como estão minhas metas?",
        "Quanto consigo poupar por mês?",
        "Qual é o meu patrimônio?",
        "Como fica meu saldo daqui a 6 meses?",
        "Como está minha saúde financeira?"
    )

    fun responder(pergunta: String, dados: DadosFinanceiros): RespostaDoAssistente {
        val texto = normalizar(pergunta)
        fun tem(vararg termos: String) = termos.any { texto.contains(it) }
        val mes = if (tem("mes passado", "ultimo mes", "mes anterior")) {
            YearMonth.from(dados.hoje).minusMonths(1)
        } else {
            YearMonth.from(dados.hoje)
        }
        return when {
            texto.isBlank() -> naoEntendi()
            tem("patrimonio") -> patrimonio(dados)
            tem("fatura", "cartao", "cartoes") -> cartoes(dados)
            tem("divida", "financiamento", "emprestimo", "devo", "devendo") -> dividas(dados)
            tem("meta", "objetivo") -> metas(dados)
            tem("orcamento") -> orcamentos(dados)
            tem("projec", "futuro", "daqui a", "vai sobrar", "proximos meses") -> projecao(dados)
            tem("poup", "sobra", "economiz", "guardar por mes") -> poupanca(dados)
            tem("maior gasto", "onde gast", "onde estou gast", "categoria", "mais gast", "gastando mais") ->
                categorias(dados, mes)
            tem("gast", "despesa") -> despesas(dados, mes)
            tem("receb", "receita", "ganh", "renda", "salario") -> receitas(dados, mes)
            tem("vence", "vencid", "a pagar", "pendente", "proximos dias") -> pendentes(dados)
            tem("alerta", "risco", "problema", "atencao", "como estou", "saude", "resumo", "diagnostico", "situacao") ->
                diagnostico(dados)
            tem("saldo", "quanto tenho", "dinheiro", "conta") -> saldos(dados)
            else -> naoEntendi()
        }
    }

    internal fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex(" +"), " ")
            .trim()

    private fun naoEntendi() = RespostaDoAssistente(
        texto = "Não reconheci o assunto dessa pergunta. Consigo responder sobre saldo, gastos, receitas, " +
            "categorias, contas a vencer, cartões, dívidas, metas, orçamentos, poupança, patrimônio, " +
            "projeção e saúde financeira.",
        premissas = listOf("O assistente local reconhece assuntos por palavras-chave; ele não interpreta texto livre."),
        entendida = false
    )

    private fun periodo(dados: DadosFinanceiros, mes: YearMonth): Pair<LocalDate, LocalDate> {
        val fim = if (mes == YearMonth.from(dados.hoje)) dados.hoje else mes.atEndOfMonth()
        return mes.atDay(1) to fim
    }

    private fun premissaDoPeriodo(inicio: LocalDate, fim: LocalDate) =
        "Lançamentos confirmados de ${inicio.formatarBR()} a ${fim.formatarBR()}. Previstos não entram."

    private fun saldos(dados: DadosFinanceiros): RespostaDoAssistente {
        if (dados.contas.isEmpty()) return RespostaDoAssistente("Você ainda não cadastrou nenhuma conta.")
        val linhas = dados.contas.map { conta ->
            "${conta.nome}: ${CalculadoraDeSaldos.saldoAtual(conta, dados.operacoes, dados.hoje).formatarBRL()}"
        }
        val total = CalculadoraDeSaldos.saldoTotal(dados.contas, dados.operacoes, dados.hoje)
        return RespostaDoAssistente(
            "Saldo total: ${total.formatarBRL()}.\n" + linhas.joinToString("\n"),
            listOf("Saldo inicial de cada conta mais os lançamentos confirmados até ${dados.hoje.formatarBR()}.")
        )
    }

    private fun despesas(dados: DadosFinanceiros, mes: YearMonth): RespostaDoAssistente {
        val (inicio, fim) = periodo(dados, mes)
        val resumo = ResumoFinanceiro.doPeriodo(dados.operacoes, inicio, fim)
        return RespostaDoAssistente(
            "Despesas em ${mes.rotulo()}: ${resumo.despesas.formatarBRL()}. " +
                "Receitas: ${resumo.receitas.formatarBRL()}. Resultado: ${resumo.resultado.formatarBRL()}.",
            listOf(
                premissaDoPeriodo(inicio, fim),
                "Compras no cartão contam na data da compra. Transferências, pagamentos de fatura e aplicações não são despesa."
            )
        )
    }

    private fun receitas(dados: DadosFinanceiros, mes: YearMonth): RespostaDoAssistente {
        val (inicio, fim) = periodo(dados, mes)
        val resumo = ResumoFinanceiro.doPeriodo(dados.operacoes, inicio, fim)
        val porCategoria = resumo.receitasPorCategoria.entries.sortedByDescending { it.value }
            .joinToString("\n") { "${dados.nomeDaCategoria(it.key)}: ${it.value.formatarBRL()}" }
        return RespostaDoAssistente(
            "Receitas em ${mes.rotulo()}: ${resumo.receitas.formatarBRL()}." +
                (if (porCategoria.isNotEmpty()) "\n$porCategoria" else ""),
            listOf(premissaDoPeriodo(inicio, fim), "Resgates de investimento e reembolsos não contam como receita.")
        )
    }

    private fun categorias(dados: DadosFinanceiros, mes: YearMonth): RespostaDoAssistente {
        val (inicio, fim) = periodo(dados, mes)
        val resumo = ResumoFinanceiro.doPeriodo(dados.operacoes, inicio, fim)
        if (!resumo.despesas.isPositivo) {
            return RespostaDoAssistente(
                "Não há despesas confirmadas em ${mes.rotulo()}.", listOf(premissaDoPeriodo(inicio, fim))
            )
        }
        val maiores = resumo.despesasPorCategoria.entries.sortedByDescending { it.value }.take(5)
            .joinToString("\n") { (categoria, valor) ->
                val parte = com.jfpfinance.engine.percentual(valor, resumo.despesas)
                "${dados.nomeDaCategoria(categoria)}: ${valor.formatarBRL()}" + (parte?.let { " ($it%)" } ?: "")
            }
        return RespostaDoAssistente(
            "Maiores despesas em ${mes.rotulo()} (total ${resumo.despesas.formatarBRL()}):\n$maiores",
            listOf(premissaDoPeriodo(inicio, fim))
        )
    }

    private fun pendentes(dados: DadosFinanceiros): RespostaDoAssistente {
        val pendentes = ContasAPagarEReceber.pendentes(dados.operacoes, dados.hoje, Analises.DIAS_DE_VENCIMENTO)
        if (pendentes.vencidos.isEmpty() && pendentes.proximos.isEmpty()) {
            return RespostaDoAssistente(
                "Nada previsto vencido nem para os próximos ${Analises.DIAS_DE_VENCIMENTO} dias.",
                listOf("Só aparecem lançamentos registrados como previstos.")
            )
        }
        fun linha(operacao: com.jfpfinance.core.model.Operacao): String {
            val efeito = dados.contas.map { operacao.efeitoNaConta(it.id) }.somar()
            val sentido = if (efeito.isPositivo) "a receber" else "a pagar"
            val nome = operacao.descricao.ifBlank { dados.nomeDaCategoria(operacao.categoriaId) }
            return "${operacao.data.formatarBR()} — $nome: ${operacao.valor.formatarBRL()} ($sentido)"
        }
        val partes = buildList {
            if (pendentes.vencidos.isNotEmpty()) add("Vencidos:\n" + pendentes.vencidos.joinToString("\n", transform = ::linha))
            if (pendentes.proximos.isNotEmpty()) {
                add("Próximos ${Analises.DIAS_DE_VENCIMENTO} dias:\n" + pendentes.proximos.joinToString("\n", transform = ::linha))
            }
        }
        return RespostaDoAssistente(
            partes.joinToString("\n\n"),
            listOf("Só aparecem lançamentos registrados como previstos. Faturas de cartão aparecem na pergunta sobre cartões.")
        )
    }

    private fun cartoes(dados: DadosFinanceiros): RespostaDoAssistente {
        if (dados.cartoes.isEmpty()) return RespostaDoAssistente("Você não tem cartões cadastrados.")
        val linhas = dados.cartoes.map { cartao ->
            val fatura = Faturas.listar(cartao, dados.operacoes).firstOrNull { !it.quitada }
            val sobreFatura = if (fatura == null) {
                "sem fatura em aberto"
            } else {
                "fatura com vencimento em ${fatura.vencimento.formatarBR()}: falta ${fatura.restante.formatarBRL()}"
            }
            "${cartao.nome}: em uso ${Faturas.limiteUtilizado(cartao, dados.operacoes).formatarBRL()} de " +
                "${cartao.limite.formatarBRL()}; $sobreFatura"
        }
        return RespostaDoAssistente(
            linhas.joinToString("\n"),
            listOf("O valor em uso inclui parcelas que ainda vão vencer. Juros e multas de atraso não são calculados.")
        )
    }

    private fun dividas(dados: DadosFinanceiros): RespostaDoAssistente {
        val cartoes = dados.cartoes.map { Faturas.limiteUtilizado(it, dados.operacoes) }.filter { it.isPositivo }.somar()
        if (dados.dividas.isEmpty() && cartoes.isZero) return RespostaDoAssistente("Você não tem dívidas cadastradas nem saldo em cartões.")
        val resumos = dados.dividas.map { it to Amortizacao.resumo(it) }
        val linhas = resumos.map { (divida, resumo) ->
            val proxima = resumo.proximaParcela
                ?.let { "; próxima parcela ${it.valor.formatarBRL()} em ${it.vencimento.formatarBR()}" } ?: "; quitada"
            "${divida.credor}: saldo devedor ${resumo.saldoDevedor.formatarBRL()}$proxima"
        }
        val total = resumos.map { it.second.saldoDevedor }.somar()
        return RespostaDoAssistente(
            "Dívidas e financiamentos: ${total.formatarBRL()}. Cartões: ${cartoes.formatarBRL()}." +
                (if (linhas.isNotEmpty()) "\n" + linhas.joinToString("\n") else ""),
            listOf(
                "Saldo devedor pela tabela de amortização e pelas parcelas marcadas como pagas.",
                "O credor pode calcular valores diferentes (seguros, tarifas, atraso)."
            )
        )
    }

    private fun metas(dados: DadosFinanceiros): RespostaDoAssistente {
        if (dados.metas.isEmpty()) return RespostaDoAssistente("Você ainda não cadastrou metas.")
        val linhas = dados.metas.map { Metas.progresso(it, dados.aportes, dados.hoje) }.map { p ->
            val complemento = when {
                p.atingida -> "atingida"
                p.prazoVencido -> "prazo vencido, falta ${p.restante.formatarBRL()}"
                p.aporteMensalNecessario != null ->
                    "falta ${p.restante.formatarBRL()}; guardando ${p.aporteMensalNecessario!!.formatarBRL()} por mês chega no prazo"
                else -> "falta ${p.restante.formatarBRL()}"
            }
            "${p.meta.nome}: ${p.percentual}% (${p.acumulado.formatarBRL()} de ${p.meta.valorAlvo.formatarBRL()}) — $complemento"
        }
        return RespostaDoAssistente(
            linhas.joinToString("\n"),
            listOf("O valor guardado é o que você marcou em cada meta; ele não é conferido com o saldo das contas.")
        )
    }

    private fun orcamentos(dados: DadosFinanceiros): RespostaDoAssistente {
        if (dados.orcamentos.isEmpty()) return RespostaDoAssistente("Você ainda não definiu orçamentos.")
        val mes = YearMonth.from(dados.hoje)
        val linhas = Orcamentos.acompanhar(dados.orcamentos, dados.operacoes, mes).map { a ->
            val situacao = when (a.situacao) {
                SituacaoDoOrcamento.ESTOURADO -> "estourado"
                SituacaoDoOrcamento.ATENCAO -> "atenção"
                SituacaoDoOrcamento.DENTRO -> "dentro do limite"
            }
            "${dados.nomeDaCategoria(a.orcamento.categoriaId)}: ${a.realizado.formatarBRL()} de " +
                "${a.orcamento.limiteMensal.formatarBRL()} (${a.percentualUsado}%) — $situacao; " +
                "disponível ${a.disponivel.formatarBRL()}"
        }
        return RespostaDoAssistente(
            "Orçamentos de ${mes.rotulo()}:\n" + linhas.joinToString("\n"),
            listOf("O disponível desconta também as despesas previstas da categoria que ainda não foram pagas.")
        )
    }

    private fun poupanca(dados: DadosFinanceiros): RespostaDoAssistente {
        val i = Analises.indicadores(dados)
        if (i.base.meses.isEmpty()) return RespostaDoAssistente(i.base.descricao())
        val taxa = i.taxaDePoupanca?.let { " Isso é $it% da receita." } ?: ""
        val texto = if (i.base.sobraMedia.isNegativo) {
            "Em média, suas despesas passam das receitas em ${(-i.base.sobraMedia).formatarBRL()} por mês."
        } else {
            "Em média, sobram ${i.base.sobraMedia.formatarBRL()} por mês.$taxa"
        }
        return RespostaDoAssistente(
            texto,
            listOf(
                "Receita média ${i.base.receitaMedia.formatarBRL()} e despesa média ${i.base.despesaMedia.formatarBRL()}.",
                i.base.descricao(),
                "É a média do que já aconteceu; não é garantia para os próximos meses."
            )
        )
    }

    private fun patrimonio(dados: DadosFinanceiros): RespostaDoAssistente {
        val pl = Patrimonio.calcular(
            saldosDasContas = dados.contas.map { CalculadoraDeSaldos.saldoAtual(it, dados.operacoes, dados.hoje) },
            bens = dados.bens,
            investimentos = dados.investimentos,
            saldosDevedoresDasDividas = dados.dividas.map { Amortizacao.resumo(it).saldoDevedor },
            dividasDosCartoes = dados.cartoes.map { Faturas.limiteUtilizado(it, dados.operacoes) }
        )
        return RespostaDoAssistente(
            "Patrimônio líquido: ${pl.liquido.formatarBRL()}.\n" +
                "Contas ${pl.contas.formatarBRL()}, investimentos ${pl.investimentos.formatarBRL()}, bens ${pl.bens.formatarBRL()}.\n" +
                "Passivos ${pl.passivos.formatarBRL()} (dívidas ${pl.dividas.formatarBRL()}, cartões ${pl.cartoes.formatarBRL()}, " +
                "contas negativas ${pl.contasNegativas.formatarBRL()}).",
            listOf("Bens e investimentos usam o último valor que você informou; não há cotação automática.")
        )
    }

    private fun projecao(dados: DadosFinanceiros): RespostaDoAssistente {
        val p = Projecoes.projetar(dados, Cenario.INTERMEDIARIO, 6)
        val aviso = p.primeiroMesNegativo?.let { " O saldo ficaria negativo em ${it.rotulo()}." } ?: ""
        return RespostaDoAssistente(
            "Mantendo a média atual, o saldo das contas sairia de ${p.saldoInicial.formatarBRL()} para " +
                "${p.saldoFinal.formatarBRL()} em ${p.pontos.last().mes.rotulo()}.$aviso " +
                "Na aba Projeções você compara outros cenários e prazos.",
            p.premissas
        )
    }

    private fun diagnostico(dados: DadosFinanceiros): RespostaDoAssistente {
        val d = Analises.diagnostico(dados)
        val importantes = d.alertas.filter { it.gravidade != Gravidade.INFO }
        val cabecalho = when {
            d.alertas.isEmpty() -> "Nenhum alerta no momento."
            importantes.isEmpty() -> "Nenhum alerta importante. Há ${d.alertas.size} observação(ões)."
            else -> "${importantes.size} ponto(s) pedem atenção."
        }
        val lista = d.alertas.take(6).joinToString("\n") { "• ${it.titulo}" }
        val i = d.indicadores
        val indicadores = buildList {
            i.taxaDePoupanca?.let { add("sobra média de $it% da receita") }
            i.mesesDeReserva?.let { add("saldo cobre ${it.toPlainString().replace('.', ',')} mês(es) de despesas") }
            i.comprometimentoComDividas?.let { add("parcelas de dívidas em $it% da receita") }
        }.joinToString("; ")
        return RespostaDoAssistente(
            listOf(cabecalho, lista, if (indicadores.isNotEmpty()) "Indicadores: $indicadores." else "")
                .filter { it.isNotBlank() }.joinToString("\n"),
            listOf(
                i.base.descricao(),
                "Os alertas seguem regras fixas do aplicativo; os detalhes de cada um estão na aba Alertas.",
                "Isto é informação para apoiar suas decisões, não aconselhamento financeiro profissional."
            )
        )
    }
}
