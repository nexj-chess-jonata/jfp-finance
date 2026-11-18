package com.jfpfinance.ui.movimentacoes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jfpfinance.core.Money
import com.jfpfinance.core.formatarBR
import com.jfpfinance.core.rotulo
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.SituacaoOperacao

private val ABAS = listOf("Lançamentos", "Pendentes", "Cartões")

@Composable
fun MovimentacoesScreen(viewModel: MovimentacoesViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var mostrarNovoLancamento by remember { mutableStateOf(false) }
    var mostrarNovoCartao by remember { mutableStateOf(false) }
    var operacaoSelecionada by remember { mutableStateOf<Operacao?>(null) }
    var cartaoParaPagar by remember { mutableStateOf<ResumoCartao?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ABAS.forEachIndexed { indice, titulo ->
                    val conteudo: @Composable () -> Unit = {
                        Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (aba == indice) {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) { conteudo() }
                    } else {
                        OutlinedButton(
                            onClick = { aba = indice },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) { conteudo() }
                    }
                }
            }

            when (aba) {
                0 -> AbaLancamentos(
                    estado = estado,
                    aoMesAnterior = viewModel::mesAnterior,
                    aoProximoMes = viewModel::proximoMes,
                    aoSelecionar = { operacaoSelecionada = it }
                )
                1 -> AbaPendentes(estado = estado, aoSelecionar = { operacaoSelecionada = it })
                else -> AbaCartoes(
                    estado = estado,
                    aoNovoCartao = { mostrarNovoCartao = true },
                    aoPagar = { cartaoParaPagar = it }
                )
            }
        }

        FloatingActionButton(
            onClick = { mostrarNovoLancamento = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Novo lançamento")
        }
    }

    if (mostrarNovoLancamento) {
        DialogoNovoLancamento(
            estado = estado,
            aoCancelar = { mostrarNovoLancamento = false },
            aoSalvar = { novo ->
                viewModel.lancar(novo)
                mostrarNovoLancamento = false
            }
        )
    }

    if (mostrarNovoCartao) {
        DialogoNovoCartao(
            aoCancelar = { mostrarNovoCartao = false },
            aoSalvar = { nome, limite, fechamento, vencimento ->
                viewModel.salvarCartao(nome, limite, fechamento, vencimento)
                mostrarNovoCartao = false
            }
        )
    }

    cartaoParaPagar?.let { resumo ->
        DialogoPagarFatura(
            resumo = resumo,
            contas = estado.contas,
            aoCancelar = { cartaoParaPagar = null },
            aoPagar = { contaId, valor ->
                viewModel.pagarFatura(resumo.cartao.id, contaId, valor)
                cartaoParaPagar = null
            }
        )
    }

    operacaoSelecionada?.let { operacao ->
        DialogoAcoes(
            operacao = operacao,
            jaEstornada = operacao.id in estado.revertidas,
            aoFechar = { operacaoSelecionada = null },
            aoEstornar = {
                viewModel.estornar(operacao)
                operacaoSelecionada = null
            },
            aoConfirmar = {
                viewModel.confirmarHoje(operacao.id)
                operacaoSelecionada = null
            },
            aoCancelarPrevista = {
                viewModel.cancelarPrevista(operacao.id)
                operacaoSelecionada = null
            }
        )
    }

    estado.erro?.let { mensagem ->
        AlertDialog(
            onDismissRequest = viewModel::limparErro,
            title = { Text("Não foi possível concluir") },
            text = { Text(mensagem) },
            confirmButton = { TextButton(onClick = viewModel::limparErro) { Text("OK") } }
        )
    }
}

@Composable
private fun AbaLancamentos(
    estado: MovimentacoesUiState,
    aoMesAnterior: () -> Unit,
    aoProximoMes: () -> Unit,
    aoSelecionar: (Operacao) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = aoMesAnterior) { Text("‹ Anterior") }
                Text(
                    estado.mes.rotulo(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                TextButton(onClick = aoProximoMes) { Text("Próximo ›") }
            }
        }
        estado.resumoDoMes?.let { resumo ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinhaResumo("Receitas confirmadas", resumo.receitas)
                        LinhaResumo("Despesas confirmadas", resumo.despesas)
                        LinhaResumo("Resultado", resumo.resultado, destaque = true)
                    }
                }
            }
        }
        if (!estado.carregando && estado.lancamentosDoMes.isEmpty()) {
            item {
                Text(
                    "Nenhum lançamento neste mês. Toque em + para registrar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(estado.lancamentosDoMes, key = { it.id }) { operacao ->
            LinhaOperacao(operacao, estado, aoSelecionar)
        }
    }
}

@Composable
private fun AbaPendentes(estado: MovimentacoesUiState, aoSelecionar: (Operacao) -> Unit) {
    val vencidos = estado.pendentes.filter { it.data.isBefore(estado.hoje) }
    val aVencer = estado.pendentes.filter { !it.data.isBefore(estado.hoje) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!estado.carregando && estado.pendentes.isEmpty()) {
            item {
                Text(
                    "Nenhuma conta a pagar ou a receber. Para criar uma, registre um lançamento " +
                        "e desmarque \"Já aconteceu\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (vencidos.isNotEmpty()) {
            item {
                Text(
                    "Vencidos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(vencidos, key = { "v-${it.id}" }) { LinhaOperacao(it, estado, aoSelecionar) }
        }
        if (aVencer.isNotEmpty()) {
            item { Text("A vencer", style = MaterialTheme.typography.titleMedium) }
            items(aVencer, key = { "a-${it.id}" }) { LinhaOperacao(it, estado, aoSelecionar) }
        }
    }
}

@Composable
private fun AbaCartoes(
    estado: MovimentacoesUiState,
    aoNovoCartao: () -> Unit,
    aoPagar: (ResumoCartao) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedButton(onClick = aoNovoCartao) { Text("Novo cartão") }
        }
        if (!estado.carregando && estado.cartoes.isEmpty()) {
            item {
                Text(
                    "Nenhum cartão cadastrado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(estado.cartoes, key = { it.cartao.id }) { resumo ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(resumo.cartao.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    LinhaResumo("Limite", resumo.cartao.limite)
                    LinhaResumo("Em uso (inclui parcelas futuras)", resumo.utilizado)
                    LinhaResumo("Disponível", resumo.disponivel, destaque = true)
                    Text(
                        "Fecha no dia ${resumo.cartao.diaFechamento} · vence no dia ${resumo.cartao.diaVencimento}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val fatura = resumo.faturaAPagar
                    if (fatura == null) {
                        Text(
                            "Nenhuma fatura em aberto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val situacao = if (fatura.fechadaEm(estado.hoje)) "fechada" else "ainda aberta"
                        Text(
                            "Fatura com vencimento em ${fatura.vencimento.formatarBR()} ($situacao): " +
                                "falta pagar ${fatura.restante.formatarBRL()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { aoPagar(resumo) }, enabled = estado.contas.isNotEmpty()) {
                            Text("Pagar fatura")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaResumo(rotulo: String, valor: Money, destaque: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            valor.formatarBRL(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (destaque) FontWeight.SemiBold else FontWeight.Normal,
            color = if (valor.isNegativo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LinhaOperacao(
    operacao: Operacao,
    estado: MovimentacoesUiState,
    aoSelecionar: (Operacao) -> Unit
) {
    val categoria = estado.categorias.firstOrNull { it.id == operacao.categoriaId }?.nome
    val conta = estado.contas.firstOrNull { it.id == operacao.contaId }?.nome
    val destino = estado.contas.firstOrNull { it.id == operacao.contaDestinoId }?.nome
    val cartao = estado.cartoes.firstOrNull { it.cartao.id == operacao.cartaoId }?.cartao?.nome

    val titulo = operacao.descricao.ifBlank { categoria ?: rotuloDoTipo(operacao.tipo) }
    val detalhes = buildList {
        add(operacao.data.formatarBR())
        if (conta != null && destino != null) add("$conta → $destino") else if (conta != null) add(conta)
        if (cartao != null) add(cartao)
        if (categoria != null && operacao.descricao.isNotBlank()) add(categoria)
        if (operacao.parcela != null && operacao.totalParcelas != null) {
            add("parcela ${operacao.parcela}/${operacao.totalParcelas}")
        }
        when {
            operacao.ehReversao -> add("estorno")
            operacao.id in estado.revertidas -> add("estornada")
            operacao.situacao == SituacaoOperacao.PREVISTA -> add("prevista")
        }
    }.joinToString(" · ")

    val sinal = sinalVisual(operacao)
    val prefixo = when {
        sinal > 0 -> "+ "
        sinal < 0 -> "- "
        else -> ""
    }
    val cor = when {
        operacao.situacao == SituacaoOperacao.PREVISTA -> MaterialTheme.colorScheme.onSurfaceVariant
        sinal > 0 -> MaterialTheme.colorScheme.primary
        sinal < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { aoSelecionar(operacao) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    detalhes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                prefixo + operacao.valor.formatarBRL(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = cor,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun DialogoAcoes(
    operacao: Operacao,
    jaEstornada: Boolean,
    aoFechar: () -> Unit,
    aoEstornar: () -> Unit,
    aoConfirmar: () -> Unit,
    aoCancelarPrevista: () -> Unit
) {
    val podeEstornar = operacao.situacao == SituacaoOperacao.CONFIRMADA && !operacao.ehReversao && !jaEstornada

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(operacao.descricao.ifBlank { rotuloDoTipo(operacao.tipo) }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${rotuloDoTipo(operacao.tipo)} · ${operacao.data.formatarBR()} · ${operacao.valor.formatarBRL()}")
                when {
                    operacao.situacao == SituacaoOperacao.PREVISTA -> {
                        TextButton(onClick = aoConfirmar) { Text("Confirmar com a data de hoje") }
                        TextButton(onClick = aoCancelarPrevista) { Text("Cancelar esta previsão") }
                    }
                    podeEstornar -> {
                        Text(
                            "Lançamentos confirmados não são apagados. O estorno anula o efeito " +
                                "e os dois registros ficam no histórico.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = aoEstornar) { Text("Estornar") }
                    }
                    operacao.ehReversao -> Text("Este registro é um estorno.", style = MaterialTheme.typography.bodySmall)
                    jaEstornada -> Text("Este lançamento já foi estornado.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = aoFechar) { Text("Fechar") } }
    )
}
