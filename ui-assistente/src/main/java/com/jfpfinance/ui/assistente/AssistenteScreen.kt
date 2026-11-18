package com.jfpfinance.ui.assistente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jfpfinance.ai.AssistenteLocal
import com.jfpfinance.core.rotulo
import com.jfpfinance.engine.Alerta
import com.jfpfinance.engine.Cenario
import com.jfpfinance.engine.Gravidade
import com.jfpfinance.engine.Indicadores
import com.jfpfinance.engine.Projecao
import com.jfpfinance.ui.comum.Abas
import com.jfpfinance.ui.comum.CampoTexto
import com.jfpfinance.ui.comum.LinhaValor
import com.jfpfinance.ui.comum.Seletor
import com.jfpfinance.ui.comum.TextoDeApoio

private val ABAS = listOf("Alertas", "Perguntar", "Projeções")

@Composable
fun AssistenteScreen(viewModel: AssistenteViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Abas(titulos = ABAS, selecionada = aba, aoSelecionar = { aba = it })
        when (aba) {
            0 -> AbaAlertas(estado)
            1 -> AbaPerguntar(estado, aoPerguntar = viewModel::perguntar, aoLimpar = viewModel::limparConversa)
            else -> AbaProjecoes(
                estado = estado,
                aoEscolherHorizonte = viewModel::escolherHorizonte,
                aoEscolherCenario = viewModel::escolherCenario
            )
        }
    }
}

// ------------------------------------------------------------------ Alertas

@Composable
private fun AbaAlertas(estado: AssistenteUiState) {
    val diagnostico = estado.diagnostico
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (diagnostico != null) {
            item { CartaoIndicadores(diagnostico.indicadores) }
            if (diagnostico.alertas.isEmpty()) {
                item { TextoDeApoio("Nenhum alerta no momento.") }
            }
            items(diagnostico.alertas) { alerta -> CartaoAlerta(alerta) }
            item {
                TextoDeApoio(
                    "Os alertas seguem regras fixas calculadas neste aparelho. São informação para apoiar " +
                        "suas decisões, não aconselhamento financeiro profissional."
                )
            }
        }
    }
}

@Composable
private fun CartaoIndicadores(indicadores: Indicadores) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Saúde financeira", style = MaterialTheme.typography.titleMedium)
            if (indicadores.base.meses.isEmpty()) {
                TextoDeApoio(indicadores.base.descricao())
            } else {
                LinhaValor("Receita média por mês", indicadores.base.receitaMedia)
                LinhaValor("Despesa média por mês", indicadores.base.despesaMedia)
                LinhaValor("Sobra média por mês", indicadores.base.sobraMedia, destaque = true)
                indicadores.taxaDePoupanca?.let { Text("Sobra $it% da receita", style = MaterialTheme.typography.bodyMedium) }
                indicadores.mesesDeReserva?.let {
                    Text(
                        "O saldo das contas cobre ${it.toPlainString().replace('.', ',')} mês(es) de despesas",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                indicadores.comprometimentoComDividas?.let {
                    Text("Parcelas de dívidas: $it% da receita", style = MaterialTheme.typography.bodyMedium)
                }
                TextoDeApoio(indicadores.base.descricao())
            }
        }
    }
}

@Composable
private fun CartaoAlerta(alerta: Alerta) {
    val (rotulo, cor) = when (alerta.gravidade) {
        Gravidade.CRITICO -> "Importante" to MaterialTheme.colorScheme.error
        Gravidade.ATENCAO -> "Atenção" to MaterialTheme.colorScheme.primary
        Gravidade.INFO -> "Observação" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(rotulo, style = MaterialTheme.typography.labelLarge, color = cor)
            Text(alerta.titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            TextoDeApoio(alerta.detalhe)
        }
    }
}

// ------------------------------------------------------------------ Perguntar

@Composable
private fun AbaPerguntar(
    estado: AssistenteUiState,
    aoPerguntar: (String) -> Unit,
    aoLimpar: () -> Unit
) {
    var texto by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TextoDeApoio(
                    "Assistente local: funciona sem internet e responde com os números calculados neste " +
                        "aparelho. Ele reconhece assuntos por palavras-chave e só consulta: não cria, altera " +
                        "nem apaga lançamentos. A IA externa ainda não está disponível nesta versão."
                )
            }
            if (estado.conversa.isEmpty()) {
                item { Text("Experimente perguntar:", style = MaterialTheme.typography.titleMedium) }
                items(AssistenteLocal.EXEMPLOS) { exemplo ->
                    TextButton(onClick = { aoPerguntar(exemplo) }) { Text(exemplo) }
                }
            } else {
                itemsIndexed(estado.conversa) { _, mensagem -> BalaoDeMensagem(mensagem) }
                item { TextButton(onClick = aoLimpar) { Text("Limpar conversa") } }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Sua pergunta") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                enabled = texto.isNotBlank() && !estado.carregando,
                onClick = {
                    aoPerguntar(texto)
                    texto = ""
                }
            ) { Text("Enviar") }
        }
    }
}

@Composable
private fun BalaoDeMensagem(mensagem: Mensagem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (mensagem.doUsuario) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (mensagem.doUsuario) "Você" else "Assistente",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(mensagem.texto, style = MaterialTheme.typography.bodyMedium)
            if (mensagem.premissas.isNotEmpty()) {
                Text("Como cheguei nisso:", style = MaterialTheme.typography.labelMedium)
                mensagem.premissas.forEach { TextoDeApoio("• $it") }
            }
        }
    }
}

// ------------------------------------------------------------------ Projeções

@Composable
private fun AbaProjecoes(
    estado: AssistenteUiState,
    aoEscolherHorizonte: (Horizonte) -> Unit,
    aoEscolherCenario: (Cenario) -> Unit
) {
    val personalizadoModelo = remember { cenarioPersonalizado(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, com.jfpfinance.core.Money.ZERO, com.jfpfinance.core.Money.ZERO) }
    val opcoes = Cenario.PADROES + personalizadoModelo
    val cenarioAtual = estado.configuracao.cenario
    val personalizado = cenarioAtual.nome == NOME_DO_CENARIO_PERSONALIZADO

    var receitaPct by remember { mutableStateOf("") }
    var despesaPct by remember { mutableStateOf("") }
    var receitaExtra by remember { mutableStateOf("") }
    var despesaExtra by remember { mutableStateOf("") }

    val vr = lerPercentual(receitaPct)
    val vd = lerPercentual(despesaPct)
    val er = lerAjuste(receitaExtra)
    val ed = lerAjuste(despesaExtra)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            TextoDeApoio("Simulação: nada aqui é gravado nem altera seus lançamentos.")
        }
        item {
            Seletor(
                rotulo = "Prazo",
                opcoes = Horizonte.entries.toList(),
                selecionado = estado.configuracao.horizonte,
                texto = { it.rotulo },
                aoSelecionar = aoEscolherHorizonte
            )
        }
        item {
            Seletor(
                rotulo = "Cenário",
                opcoes = opcoes,
                selecionado = opcoes.firstOrNull { it.nome == cenarioAtual.nome },
                texto = { it.nome },
                aoSelecionar = aoEscolherCenario
            )
        }
        if (personalizado) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTexto(receitaPct, { receitaPct = it }, "Variação da receita (%) — ex.: -20", invalido = vr == null)
                    CampoTexto(despesaPct, { despesaPct = it }, "Variação da despesa (%) — ex.: 10", invalido = vd == null)
                    CampoTexto(receitaExtra, { receitaExtra = it }, "Receita a mais ou a menos por mês (R$) — ex.: -1500", invalido = er == null)
                    CampoTexto(despesaExtra, { despesaExtra = it }, "Despesa a mais ou a menos por mês (R$) — ex.: 300", invalido = ed == null)
                    Button(
                        enabled = vr != null && vd != null && er != null && ed != null,
                        onClick = {
                            if (vr != null && vd != null && er != null && ed != null) {
                                aoEscolherCenario(cenarioPersonalizado(vr, vd, er, ed))
                            }
                        }
                    ) { Text("Simular") }
                }
            }
        }
        estado.projecao?.let { projecao ->
            item { CartaoResumoDaProjecao(projecao) }
            items(projecao.pontos, key = { it.mes.toString() }) { ponto ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ponto.mes.rotulo(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        ponto.saldoFinal.formatarBRL(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ponto.saldoFinal.isNegativo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            item { Text("Premissas", style = MaterialTheme.typography.titleMedium) }
            items(projecao.premissas) { premissa -> TextoDeApoio("• $premissa") }
        }
    }
}

@Composable
private fun CartaoResumoDaProjecao(projecao: Projecao) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Projeção — cenário ${projecao.cenario.nome}", style = MaterialTheme.typography.titleMedium)
            LinhaValor("Saldo das contas hoje", projecao.saldoInicial)
            projecao.pontos.firstOrNull()?.let { ponto ->
                LinhaValor("Receita projetada por mês", ponto.receitas)
                LinhaValor("Despesa projetada por mês", ponto.despesas)
            }
            LinhaValor("Saldo projetado ao fim do prazo", projecao.saldoFinal, destaque = true)
            val negativo = projecao.primeiroMesNegativo
            if (negativo != null) {
                TextoDeApoio("O saldo ficaria negativo em ${negativo.rotulo()}.", erro = true)
            }
            if (projecao.base.meses.isEmpty()) {
                TextoDeApoio("Sem lançamentos confirmados, a projeção repete o saldo atual.", erro = true)
            } else if (projecao.base.incompleta) {
                TextoDeApoio("Pouco histórico: a base é só o mês atual, que ainda não terminou.", erro = true)
            }
        }
    }
}
