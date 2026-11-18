package com.jfpfinance.ui.patrimonio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.jfpfinance.core.Money
import com.jfpfinance.core.formatarBR
import com.jfpfinance.core.lerDataBR
import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.TipoBem
import com.jfpfinance.core.model.TipoInvestimento
import com.jfpfinance.core.model.TipoMovimentoInvestimento
import com.jfpfinance.engine.PatrimonioLiquido
import com.jfpfinance.engine.PosicaoDoInvestimento
import com.jfpfinance.ui.comum.Abas
import com.jfpfinance.ui.comum.CampoTexto
import com.jfpfinance.ui.comum.CampoValor
import com.jfpfinance.ui.comum.DialogoDeConfirmacao
import com.jfpfinance.ui.comum.DialogoDeErro
import com.jfpfinance.ui.comum.LinhaValor
import com.jfpfinance.ui.comum.Seletor
import com.jfpfinance.ui.comum.TextoDeApoio
import java.math.BigDecimal
import java.time.LocalDate

private val ABAS = listOf("Resumo", "Bens", "Investimentos")

private class Confirmacao(val titulo: String, val mensagem: String, val acao: () -> Unit)

private class PedidoDeMovimento(val posicao: PosicaoDoInvestimento, val tipo: TipoMovimentoInvestimento)

@Composable
fun PatrimonioScreen(viewModel: PatrimonioViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var mostrarNovoBem by remember { mutableStateOf(false) }
    var mostrarNovoInvestimento by remember { mutableStateOf(false) }
    var reavaliarBem by remember { mutableStateOf<Bem?>(null) }
    var reavaliarInvestimento by remember { mutableStateOf<PosicaoDoInvestimento?>(null) }
    var movimento by remember { mutableStateOf<PedidoDeMovimento?>(null) }
    var confirmacao by remember { mutableStateOf<Confirmacao?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Abas(titulos = ABAS, selecionada = aba, aoSelecionar = { aba = it })
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (aba) {
                0 -> estado.patrimonio?.let { patrimonio ->
                    item { CartaoPatrimonio(patrimonio) }
                    item {
                        TextoDeApoio(
                            "Valores de bens e investimentos são os que você informou por último; " +
                                "atualize-os para manter o patrimônio correto."
                        )
                    }
                }

                1 -> {
                    item { OutlinedButton(onClick = { mostrarNovoBem = true }) { Text("Novo bem") } }
                    if (!estado.carregando && estado.bens.isEmpty()) {
                        item { TextoDeApoio("Nenhum bem cadastrado.") }
                    }
                    items(estado.bens, key = { it.id }) { bem ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(bem.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                TextoDeApoio("${bem.tipo.rotulo} · adquirido em ${bem.dataAquisicao.formatarBR()}")
                                LinhaValor("Valor de aquisição", bem.valorAquisicao)
                                LinhaValor("Valor estimado atual", bem.valorAtual, destaque = true)
                                LinhaValor("Valorização ou depreciação", bem.valorAtual - bem.valorAquisicao)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { reavaliarBem = bem }) { Text("Atualizar valor") }
                                    TextButton(onClick = {
                                        confirmacao = Confirmacao(
                                            "Arquivar bem",
                                            "Arquivar \"${bem.nome}\"? Ele deixa de contar no patrimônio."
                                        ) { viewModel.arquivarBem(bem.id) }
                                    }) { Text("Arquivar") }
                                }
                            }
                        }
                    }
                }

                else -> {
                    item {
                        OutlinedButton(onClick = { mostrarNovoInvestimento = true }) { Text("Novo investimento") }
                    }
                    if (!estado.carregando && estado.posicoes.isEmpty()) {
                        item { TextoDeApoio("Nenhum investimento cadastrado.") }
                    }
                    items(estado.posicoes, key = { it.investimento.id }) { posicao ->
                        CartaoInvestimento(
                            posicao = posicao,
                            aoMovimentar = { tipo -> movimento = PedidoDeMovimento(posicao, tipo) },
                            aoReavaliar = { reavaliarInvestimento = posicao },
                            aoArquivar = {
                                confirmacao = Confirmacao(
                                    "Arquivar investimento",
                                    "Arquivar \"${posicao.investimento.nome}\"? Ele deixa de contar no patrimônio."
                                ) { viewModel.arquivarInvestimento(posicao.investimento.id) }
                            }
                        )
                    }
                    item {
                        TextoDeApoio(
                            "Ganho não realizado é uma estimativa com base no valor atual informado. " +
                                "Nenhum valor aqui é promessa de rentabilidade."
                        )
                    }
                }
            }
        }
    }

    if (mostrarNovoBem) {
        DialogoNovoBem(
            hoje = estado.hoje,
            aoCancelar = { mostrarNovoBem = false },
            aoSalvar = { nome, tipo, aquisicao, data, atual ->
                viewModel.novoBem(nome, tipo, aquisicao, data, atual)
                mostrarNovoBem = false
            }
        )
    }
    if (mostrarNovoInvestimento) {
        DialogoNovoInvestimento(
            aoCancelar = { mostrarNovoInvestimento = false },
            aoSalvar = { nome, tipo ->
                viewModel.novoInvestimento(nome, tipo)
                mostrarNovoInvestimento = false
            }
        )
    }
    reavaliarBem?.let { bem ->
        DialogoValorAtual(
            titulo = "Valor atual — ${bem.nome}",
            valorInicial = bem.valorAtual,
            aoCancelar = { reavaliarBem = null },
            aoSalvar = { valor ->
                viewModel.atualizarValorDoBem(bem, valor)
                reavaliarBem = null
            }
        )
    }
    reavaliarInvestimento?.let { posicao ->
        DialogoValorAtual(
            titulo = "Valor atual — ${posicao.investimento.nome}",
            valorInicial = posicao.investimento.valorAtual,
            aoCancelar = { reavaliarInvestimento = null },
            aoSalvar = { valor ->
                viewModel.atualizarValorDoInvestimento(posicao.investimento, valor)
                reavaliarInvestimento = null
            }
        )
    }
    movimento?.let { pedido ->
        DialogoMovimento(
            pedido = pedido,
            hoje = estado.hoje,
            contas = estado.contas,
            aoCancelar = { movimento = null },
            aoSalvar = { valor, data, quantidade, contaId ->
                viewModel.movimentar(pedido.posicao.investimento, pedido.tipo, valor, data, quantidade, contaId)
                movimento = null
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
private fun CartaoPatrimonio(patrimonio: PatrimonioLiquido) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Patrimônio líquido", style = MaterialTheme.typography.labelLarge)
            Text(
                patrimonio.liquido.formatarBRL(),
                style = MaterialTheme.typography.headlineMedium,
                color = if (patrimonio.liquido.isNegativo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("O que você tem", style = MaterialTheme.typography.titleMedium)
            LinhaValor("Contas", patrimonio.contas)
            LinhaValor("Investimentos", patrimonio.investimentos)
            LinhaValor("Bens", patrimonio.bens)
            LinhaValor("Patrimônio bruto", patrimonio.bruto, destaque = true)
        }
    }
    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("O que você deve", style = MaterialTheme.typography.titleMedium)
            LinhaValor("Dívidas e financiamentos", patrimonio.dividas)
            LinhaValor("Cartões (inclui parcelas a vencer)", patrimonio.cartoes)
            LinhaValor("Contas com saldo negativo", patrimonio.contasNegativas)
            LinhaValor("Total de passivos", patrimonio.passivos, destaque = true)
        }
    }
}

@Composable
private fun CartaoInvestimento(
    posicao: PosicaoDoInvestimento,
    aoMovimentar: (TipoMovimentoInvestimento) -> Unit,
    aoReavaliar: () -> Unit,
    aoArquivar: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(posicao.investimento.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            TextoDeApoio(posicao.investimento.tipo.rotulo)
            LinhaValor("Valor atual informado", posicao.investimento.valorAtual, destaque = true)
            LinhaValor("Custo do que está aplicado", posicao.custo)
            LinhaValor("Ganho não realizado (estimativa)", posicao.ganhoNaoRealizado)
            LinhaValor("Ganho já realizado", posicao.ganhoRealizado)
            if (posicao.quantidade.signum() > 0) {
                TextoDeApoio("Quantidade: ${posicao.quantidade.stripTrailingZeros().toPlainString().replace('.', ',')}")
                posicao.precoMedio?.let { LinhaValor("Preço médio", it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { aoMovimentar(TipoMovimentoInvestimento.APORTE) }) { Text("Aportar") }
                OutlinedButton(
                    onClick = { aoMovimentar(TipoMovimentoInvestimento.RESGATE) },
                    enabled = posicao.investimento.valorAtual.isPositivo
                ) { Text("Resgatar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { aoMovimentar(TipoMovimentoInvestimento.RENDIMENTO) }) { Text("Rendimento") }
                TextButton(onClick = aoReavaliar) { Text("Atualizar valor") }
                TextButton(onClick = aoArquivar) { Text("Arquivar") }
            }
        }
    }
}

@Composable
private fun DialogoNovoBem(
    hoje: LocalDate,
    aoCancelar: () -> Unit,
    aoSalvar: (nome: String, tipo: TipoBem, valorAquisicao: Money, dataAquisicao: LocalDate, valorAtual: Money) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoBem.IMOVEL) }
    var aquisicaoTexto by remember { mutableStateOf("") }
    var dataTexto by remember { mutableStateOf(hoje.formatarBR()) }
    var atualTexto by remember { mutableStateOf("") }

    val aquisicao = Money.deTexto(aquisicaoTexto)?.takeIf { !it.isNegativo }
    val data = lerDataBR(dataTexto)
    // Valor atual em branco = igual ao valor de aquisição.
    val atual = if (atualTexto.isBlank()) aquisicao else Money.deTexto(atualTexto)?.takeIf { !it.isNegativo }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Novo bem") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CampoTexto(nome, { nome = it }, "Nome do bem")
                Seletor(
                    rotulo = "Tipo",
                    opcoes = TipoBem.entries.toList(),
                    selecionado = tipo,
                    texto = { it.rotulo },
                    aoSelecionar = { tipo = it }
                )
                CampoValor(aquisicaoTexto, { aquisicaoTexto = it }, "Valor de aquisição (R$)", invalido = aquisicaoTexto.isNotBlank() && aquisicao == null)
                CampoTexto(dataTexto, { dataTexto = it }, "Data de aquisição (dd/mm/aaaa)", invalido = data == null)
                CampoValor(atualTexto, { atualTexto = it }, "Valor estimado atual (R$) — opcional", invalido = atualTexto.isNotBlank() && atual == null)
                TextoDeApoio("Se o bem é financiado, cadastre o financiamento em Planejamento > Dívidas. Assim o bem e a dívida aparecem separados no patrimônio.")
            }
        },
        confirmButton = {
            TextButton(
                enabled = nome.isNotBlank() && aquisicao != null && data != null && atual != null,
                onClick = {
                    if (aquisicao != null && data != null && atual != null) aoSalvar(nome, tipo, aquisicao, data, atual)
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoNovoInvestimento(
    aoCancelar: () -> Unit,
    aoSalvar: (nome: String, tipo: TipoInvestimento) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoInvestimento.RENDA_FIXA) }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Novo investimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTexto(nome, { nome = it }, "Nome (ex.: CDB do banco, código da ação)")
                Seletor(
                    rotulo = "Tipo",
                    opcoes = TipoInvestimento.entries.toList(),
                    selecionado = tipo,
                    texto = { it.rotulo },
                    aoSelecionar = { tipo = it }
                )
                TextoDeApoio("Depois de salvar, use \"Aportar\" para registrar quanto foi aplicado.")
            }
        },
        confirmButton = {
            TextButton(enabled = nome.isNotBlank(), onClick = { aoSalvar(nome, tipo) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoValorAtual(
    titulo: String,
    valorInicial: Money,
    aoCancelar: () -> Unit,
    aoSalvar: (Money) -> Unit
) {
    var texto by remember { mutableStateOf(valorInicial.toBigDecimal().toPlainString().replace('.', ',')) }
    val valor = Money.deTexto(texto)?.takeIf { !it.isNegativo }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoValor(texto, { texto = it }, "Quanto vale hoje (R$)", invalido = valor == null)
                TextoDeApoio("É uma avaliação sua. Não movimenta contas nem muda o custo de aquisição.")
            }
        },
        confirmButton = {
            TextButton(enabled = valor != null, onClick = { if (valor != null) aoSalvar(valor) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogoMovimento(
    pedido: PedidoDeMovimento,
    hoje: LocalDate,
    contas: List<Conta>,
    aoCancelar: () -> Unit,
    aoSalvar: (valor: Money, data: LocalDate, quantidade: BigDecimal?, contaId: Long?) -> Unit
) {
    val tipo = pedido.tipo
    val opcoes: List<Conta?> = listOf<Conta?>(null) + contas
    var valorTexto by remember { mutableStateOf("") }
    var dataTexto by remember { mutableStateOf(hoje.formatarBR()) }
    var quantidadeTexto by remember { mutableStateOf("") }
    var conta by remember { mutableStateOf(contas.firstOrNull()) }

    val valor = Money.deTexto(valorTexto)?.takeIf { it.isPositivo }
    val data = lerDataBR(dataTexto)
    val quantidade = if (quantidadeTexto.isBlank()) null else lerQuantidade(quantidadeTexto)
    val quantidadeInvalida = quantidadeTexto.isNotBlank() && quantidade == null
    val usaQuantidade = tipo != TipoMovimentoInvestimento.RENDIMENTO

    val rotuloDaConta = when (tipo) {
        TipoMovimentoInvestimento.APORTE -> "Dinheiro sai de"
        else -> "Dinheiro entra em"
    }
    val explicacao = when (tipo) {
        TipoMovimentoInvestimento.APORTE ->
            "Escolhendo uma conta, o valor sai dela sem contar como despesa: o dinheiro só muda de lugar."
        TipoMovimentoInvestimento.RESGATE ->
            "Escolhendo uma conta, o valor entra nela sem contar como receita. " +
                "O ganho ou a perda do resgate aparece em \"Ganho já realizado\"."
        TipoMovimentoInvestimento.RENDIMENTO ->
            "Juros, dividendos ou aluguéis recebidos em dinheiro. Escolhendo uma conta, entra como receita."
    }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("${tipo.rotulo} — ${pedido.posicao.investimento.nome}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CampoValor(valorTexto, { valorTexto = it }, "Valor (R$)", invalido = valorTexto.isNotBlank() && valor == null)
                CampoTexto(dataTexto, { dataTexto = it }, "Data (dd/mm/aaaa)", invalido = data == null)
                if (usaQuantidade) {
                    CampoTexto(quantidadeTexto, { quantidadeTexto = it }, "Quantidade de cotas/ações — opcional", invalido = quantidadeInvalida)
                }
                Seletor(
                    rotulo = rotuloDaConta,
                    opcoes = opcoes,
                    selecionado = conta,
                    texto = { it?.nome ?: "não movimentar conta" },
                    aoSelecionar = { conta = it },
                    textoVazio = "não movimentar conta"
                )
                TextoDeApoio(explicacao)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valor != null && data != null && !quantidadeInvalida,
                onClick = {
                    if (valor != null && data != null) {
                        aoSalvar(valor, data, if (usaQuantidade) quantidade else null, conta?.id)
                    }
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}
