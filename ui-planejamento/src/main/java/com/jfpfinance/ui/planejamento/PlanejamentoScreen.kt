package com.jfpfinance.ui.planejamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jfpfinance.core.formatarBR
import com.jfpfinance.core.rotulo
import com.jfpfinance.engine.ProgressoDaMeta
import com.jfpfinance.engine.SituacaoDoOrcamento
import com.jfpfinance.ui.comum.Abas
import com.jfpfinance.ui.comum.DialogoDeConfirmacao
import com.jfpfinance.ui.comum.DialogoDeErro
import com.jfpfinance.ui.comum.LinhaValor
import com.jfpfinance.ui.comum.TextoDeApoio

private val ABAS = listOf("Orçamento", "Metas", "Fluxo", "Dívidas")

private class Confirmacao(val titulo: String, val mensagem: String, val acao: () -> Unit)

@Composable
fun PlanejamentoScreen(viewModel: PlanejamentoViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var mostrarOrcamento by remember { mutableStateOf(false) }
    var mostrarNovaMeta by remember { mutableStateOf(false) }
    var mostrarNovaDivida by remember { mutableStateOf(false) }
    var guardarEm by remember { mutableStateOf<ProgressoDaMeta?>(null) }
    var retirarDe by remember { mutableStateOf<ProgressoDaMeta?>(null) }
    var pagarParcelaDe by remember { mutableStateOf<DividaNaTela?>(null) }
    var confirmacao by remember { mutableStateOf<Confirmacao?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Abas(titulos = ABAS, selecionada = aba, aoSelecionar = { aba = it })
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (aba) {
                0 -> {
                    item {
                        TextoDeApoio("Limites de despesa por categoria em ${estado.hoje.let { java.time.YearMonth.from(it).rotulo() }}.")
                    }
                    item {
                        OutlinedButton(
                            onClick = { mostrarOrcamento = true },
                            enabled = estado.categoriasDeDespesa.isNotEmpty()
                        ) { Text("Definir orçamento") }
                    }
                    if (!estado.carregando && estado.orcamentos.isEmpty()) {
                        item { TextoDeApoio("Nenhum orçamento definido.") }
                    }
                    items(estado.orcamentos, key = { it.acompanhamento.orcamento.id }) { item ->
                        CartaoOrcamento(item) {
                            confirmacao = Confirmacao(
                                "Remover orçamento",
                                "Remover o orçamento de ${item.nomeDaCategoria}? Os lançamentos não são alterados."
                            ) { viewModel.removerOrcamento(item.acompanhamento.orcamento.id) }
                        }
                    }
                }

                1 -> {
                    item { OutlinedButton(onClick = { mostrarNovaMeta = true }) { Text("Nova meta") } }
                    if (!estado.carregando && estado.metas.isEmpty()) {
                        item { TextoDeApoio("Nenhuma meta cadastrada.") }
                    }
                    items(estado.metas, key = { it.meta.id }) { progresso ->
                        CartaoMeta(
                            progresso = progresso,
                            aoGuardar = { guardarEm = progresso },
                            aoRetirar = { retirarDe = progresso },
                            aoArquivar = {
                                confirmacao = Confirmacao(
                                    "Arquivar meta",
                                    "Arquivar a meta \"${progresso.meta.nome}\"? Ela sai da lista, mas o histórico é mantido."
                                ) { viewModel.arquivarMeta(progresso.meta.id) }
                            }
                        )
                    }
                }

                2 -> {
                    item {
                        TextoDeApoio(
                            "Realizado é o que já aconteceu. Previsto e saldo projetado são estimativas: " +
                                "dependem de tudo o que está lançado como previsto acontecer."
                        )
                    }
                    items(estado.fluxo, key = { it.mes.toString() }) { mes ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(mes.mes.rotulo(), style = MaterialTheme.typography.titleMedium)
                                if (mes == estado.fluxo.first()) {
                                    LinhaValor("Entradas realizadas", mes.entradasRealizadas)
                                    LinhaValor("Saídas realizadas", mes.saidasRealizadas)
                                }
                                LinhaValor("Entradas previstas", mes.entradasPrevistas)
                                LinhaValor("Saídas previstas", mes.saidasPrevistas)
                                LinhaValor("Saldo projetado no fim do mês", mes.saldoFinalProjetado, destaque = true)
                            }
                        }
                    }
                }

                else -> {
                    item { OutlinedButton(onClick = { mostrarNovaDivida = true }) { Text("Nova dívida ou simulação") } }
                    if (!estado.carregando && estado.dividas.isEmpty()) {
                        item { TextoDeApoio("Nenhuma dívida cadastrada.") }
                    }
                    items(estado.dividas, key = { it.divida.id }) { item ->
                        CartaoDivida(
                            item = item,
                            aoPagar = { pagarParcelaDe = item },
                            aoArquivar = {
                                confirmacao = Confirmacao(
                                    "Arquivar dívida",
                                    "Arquivar a dívida com ${item.divida.credor}? Ela deixa de contar no patrimônio."
                                ) { viewModel.arquivarDivida(item.divida.id) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (mostrarOrcamento) {
        DialogoOrcamento(
            categorias = estado.categoriasDeDespesa,
            aoCancelar = { mostrarOrcamento = false },
            aoSalvar = { categoriaId, limite ->
                viewModel.definirOrcamento(categoriaId, limite)
                mostrarOrcamento = false
            }
        )
    }
    if (mostrarNovaMeta) {
        DialogoNovaMeta(
            aoCancelar = { mostrarNovaMeta = false },
            aoSalvar = { nome, tipo, alvo, prazo ->
                viewModel.novaMeta(nome, tipo, alvo, prazo)
                mostrarNovaMeta = false
            }
        )
    }
    guardarEm?.let { progresso ->
        DialogoValor(
            titulo = "Guardar em \"${progresso.meta.nome}\"",
            explicacao = "Marca quanto do seu dinheiro está reservado para esta meta. Não movimenta nenhuma conta.",
            aoCancelar = { guardarEm = null },
            aoSalvar = { valor ->
                viewModel.movimentarMeta(progresso, valor)
                guardarEm = null
            }
        )
    }
    retirarDe?.let { progresso ->
        DialogoValor(
            titulo = "Retirar de \"${progresso.meta.nome}\"",
            explicacao = "Guardado até agora: ${progresso.acumulado.formatarBRL()}.",
            aoCancelar = { retirarDe = null },
            aoSalvar = { valor ->
                viewModel.movimentarMeta(progresso, -valor)
                retirarDe = null
            }
        )
    }
    if (mostrarNovaDivida) {
        DialogoNovaDivida(
            hoje = estado.hoje,
            aoCancelar = { mostrarNovaDivida = false },
            aoSalvar = { credor, valor, taxa, parcelas, sistema, primeira ->
                viewModel.novaDivida(credor, valor, taxa, parcelas, sistema, primeira)
                mostrarNovaDivida = false
            }
        )
    }
    pagarParcelaDe?.let { item ->
        DialogoPagarParcela(
            item = item,
            contas = estado.contas,
            aoCancelar = { pagarParcelaDe = null },
            aoPagar = { contaId ->
                viewModel.pagarParcela(item, contaId)
                pagarParcelaDe = null
            }
        )
    }
    confirmacao?.let { pedido ->
        DialogoDeConfirmacao(
            titulo = pedido.titulo,
            mensagem = pedido.mensagem,
            aoConfirmar = {
                pedido.acao()
                confirmacao = null
            },
            aoCancelar = { confirmacao = null }
        )
    }
    estado.erro?.let { DialogoDeErro(it, viewModel::limparErro) }
}

@Composable
private fun CartaoOrcamento(item: OrcamentoNaTela, aoRemover: () -> Unit) {
    val a = item.acompanhamento
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.nomeDaCategoria, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            LinearProgressIndicator(
                progress = { (a.percentualUsado / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (a.situacao == SituacaoDoOrcamento.DENTRO) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            LinhaValor("Limite do mês", a.orcamento.limiteMensal)
            LinhaValor("Gasto (${a.percentualUsado}%)", a.realizado)
            LinhaValor("Previsto ainda não pago", a.comprometido)
            LinhaValor("Disponível", a.disponivel, destaque = true)
            when (a.situacao) {
                SituacaoDoOrcamento.ESTOURADO -> TextoDeApoio("O gasto passou do limite.", erro = true)
                SituacaoDoOrcamento.ATENCAO -> TextoDeApoio("Atenção: perto do limite ou já comprometido.", erro = true)
                SituacaoDoOrcamento.DENTRO -> Unit
            }
            TextButton(onClick = aoRemover) { Text("Remover") }
        }
    }
}

@Composable
private fun CartaoMeta(
    progresso: ProgressoDaMeta,
    aoGuardar: () -> Unit,
    aoRetirar: () -> Unit,
    aoArquivar: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(progresso.meta.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            TextoDeApoio(progresso.meta.tipo.rotulo)
            LinearProgressIndicator(
                progress = { (progresso.percentual / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            LinhaValor("Guardado (${progresso.percentual}%)", progresso.acumulado)
            LinhaValor("Objetivo", progresso.meta.valorAlvo)
            LinhaValor("Falta", progresso.restante, destaque = true)
            val prazo = progresso.meta.prazo
            when {
                progresso.atingida -> TextoDeApoio("Meta atingida.")
                prazo == null -> TextoDeApoio("Sem prazo definido.")
                progresso.prazoVencido -> TextoDeApoio("O prazo (${prazo.formatarBR()}) já passou.", erro = true)
                else -> {
                    TextoDeApoio("Prazo: ${prazo.formatarBR()} (${progresso.mesesAtePrazo} meses)")
                    progresso.aporteMensalNecessario?.let { LinhaValor("Guardar por mês para chegar lá", it) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = aoGuardar) { Text("Guardar") }
                OutlinedButton(onClick = aoRetirar, enabled = progresso.acumulado.isPositivo) { Text("Retirar") }
                TextButton(onClick = aoArquivar) { Text("Arquivar") }
            }
        }
    }
}

@Composable
private fun CartaoDivida(item: DividaNaTela, aoPagar: () -> Unit, aoArquivar: () -> Unit) {
    val divida = item.divida
    val resumo = item.resumo
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(divida.credor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            TextoDeApoio(
                "${divida.sistema.rotulo} · ${divida.taxaMensalPercentual.toPlainString().replace('.', ',')}% ao mês · " +
                    "${resumo.parcelasPagas} de ${resumo.totalDeParcelas} parcelas pagas"
            )
            LinhaValor("Valor original", divida.valorOriginal)
            LinhaValor("Saldo devedor", resumo.saldoDevedor, destaque = true)
            LinhaValor("Juros no contrato inteiro", resumo.jurosTotais)
            LinhaValor("Custo total", resumo.custoTotal)
            val proxima = resumo.proximaParcela
            if (proxima == null) {
                TextoDeApoio("Dívida quitada.")
            } else {
                LinhaValor("Próxima parcela (${proxima.numero}, vence ${proxima.vencimento.formatarBR()})", proxima.valor)
                TextoDeApoio(
                    "Quitar o saldo agora evitaria cerca de ${resumo.jurosRestantes.formatarBRL()} em juros " +
                        "(estimativa pela tabela; o credor pode calcular diferente)."
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = aoPagar, enabled = proxima != null) { Text("Parcela paga") }
                TextButton(onClick = aoArquivar) { Text("Arquivar") }
            }
        }
    }
}
