package com.jfpfinance.ui.movimentacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jfpfinance.core.Money
import com.jfpfinance.core.formatarBR
import com.jfpfinance.core.lerDataBR
import com.jfpfinance.ui.comum.CampoNumero
import com.jfpfinance.ui.comum.CampoValor
import com.jfpfinance.ui.comum.Seletor
import com.jfpfinance.core.model.Conta
import androidx.compose.ui.unit.dp

@Composable
fun DialogoNovoLancamento(
    estado: MovimentacoesUiState,
    aoCancelar: () -> Unit,
    aoSalvar: (NovoLancamento) -> Unit
) {
    var tipo by remember { mutableStateOf(TipoLancamento.DESPESA) }
    var valorTexto by remember { mutableStateOf("") }
    var dataTexto by remember { mutableStateOf(estado.hoje.formatarBR()) }
    var contaId by remember { mutableStateOf(estado.contas.firstOrNull()?.id) }
    var contaDestinoId by remember { mutableStateOf<Long?>(null) }
    var cartaoId by remember { mutableStateOf(estado.cartoes.firstOrNull()?.cartao?.id) }
    var categoriaId by remember { mutableStateOf<Long?>(null) }
    var descricao by remember { mutableStateOf("") }
    var pessoaId by remember { mutableStateOf<Long?>(null) }
    var jaAconteceu by remember { mutableStateOf(true) }
    var vezesTexto by remember { mutableStateOf("1") }

    val valor = Money.deTexto(valorTexto)?.takeIf { it.isPositivo }
    val data = lerDataBR(dataTexto)
    val vezes = vezesTexto.toIntOrNull()?.takeIf { it in 1..120 }
    val categorias = categoriasPara(tipo, estado.categorias)
    val cartoes = estado.cartoes.map { it.cartao }

    val destinoValido = tipo != TipoLancamento.TRANSFERENCIA ||
        (contaDestinoId != null && contaDestinoId != contaId)
    val origemValida = if (tipo == TipoLancamento.COMPRA_CARTAO) cartaoId != null else contaId != null
    val podeSalvar = valor != null && data != null && vezes != null && origemValida && destinoValido

    val aviso = when {
        tipo == TipoLancamento.COMPRA_CARTAO && cartoes.isEmpty() -> "Cadastre um cartão na aba Cartões."
        tipo != TipoLancamento.COMPRA_CARTAO && estado.contas.isEmpty() -> "Cadastre uma conta na tela Início."
        tipo == TipoLancamento.TRANSFERENCIA && estado.contas.size < 2 -> "A transferência precisa de duas contas."
        else -> null
    }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Novo lançamento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Seletor(
                    rotulo = "Tipo",
                    opcoes = TipoLancamento.entries.toList(),
                    selecionado = tipo,
                    texto = { it.rotulo },
                    aoSelecionar = {
                        tipo = it
                        categoriaId = null
                    }
                )
                CampoValor(valorTexto, { valorTexto = it }, "Valor (R$)", invalido = valorTexto.isNotBlank() && valor == null)
                OutlinedTextField(
                    value = dataTexto,
                    onValueChange = { dataTexto = it },
                    label = { Text("Data (dd/mm/aaaa)") },
                    singleLine = true,
                    isError = data == null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (tipo == TipoLancamento.COMPRA_CARTAO) {
                    Seletor(
                        rotulo = "Cartão",
                        opcoes = cartoes,
                        selecionado = cartoes.firstOrNull { it.id == cartaoId },
                        texto = { it.nome },
                        aoSelecionar = { cartaoId = it.id }
                    )
                } else {
                    Seletor(
                        rotulo = if (tipo == TipoLancamento.TRANSFERENCIA) "Da conta" else "Conta",
                        opcoes = estado.contas,
                        selecionado = estado.contas.firstOrNull { it.id == contaId },
                        texto = { it.nome },
                        aoSelecionar = { contaId = it.id }
                    )
                }
                if (tipo == TipoLancamento.TRANSFERENCIA) {
                    val destinos = estado.contas.filter { it.id != contaId }
                    Seletor(
                        rotulo = "Para a conta",
                        opcoes = destinos,
                        selecionado = destinos.firstOrNull { it.id == contaDestinoId },
                        texto = { it.nome },
                        aoSelecionar = { contaDestinoId = it.id }
                    )
                } else {
                    Seletor(
                        rotulo = "Categoria",
                        opcoes = categorias,
                        selecionado = categorias.firstOrNull { it.id == categoriaId },
                        texto = { it.nome },
                        aoSelecionar = { categoriaId = it.id },
                        textoVazio = "sem categoria"
                    )
                }

                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (estado.pessoas.isNotEmpty() && tipo != TipoLancamento.TRANSFERENCIA) {
                    val opcoesDePessoa: List<com.jfpfinance.core.model.Pessoa?> =
                        listOf<com.jfpfinance.core.model.Pessoa?>(null) + estado.pessoas
                    Seletor(
                        rotulo = "De quem",
                        opcoes = opcoesDePessoa,
                        selecionado = estado.pessoas.firstOrNull { it.id == pessoaId },
                        texto = { it?.nome ?: "da casa toda" },
                        aoSelecionar = { pessoaId = it?.id },
                        textoVazio = "da casa toda"
                    )
                }

                when (tipo) {
                    TipoLancamento.COMPRA_CARTAO ->
                        CampoNumero(vezesTexto, { vezesTexto = it }, "Parcelas", invalido = vezes == null)

                    TipoLancamento.TRANSFERENCIA -> LinhaJaAconteceu(jaAconteceu) { jaAconteceu = it }

                    else -> {
                        LinhaJaAconteceu(jaAconteceu) { jaAconteceu = it }
                        CampoNumero(vezesTexto, { vezesTexto = it }, "Repetir por quantos meses", invalido = vezes == null)
                    }
                }

                if (aviso != null) {
                    Text(aviso, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = podeSalvar,
                onClick = {
                    if (valor != null && data != null && vezes != null) {
                        aoSalvar(
                            NovoLancamento(
                                tipo = tipo,
                                valor = valor,
                                data = data,
                                contaId = contaId,
                                contaDestinoId = contaDestinoId,
                                cartaoId = cartaoId,
                                categoriaId = categoriaId,
                                descricao = descricao,
                                pessoaId = if (tipo == TipoLancamento.TRANSFERENCIA) null else pessoaId,
                                jaAconteceu = jaAconteceu,
                                vezes = vezes
                            )
                        )
                    }
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun LinhaJaAconteceu(marcado: Boolean, aoMudar: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Já aconteceu", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Desmarque para registrar como conta a pagar ou a receber.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = marcado, onCheckedChange = aoMudar)
    }
}

@Composable
fun DialogoNovoCartao(
    aoCancelar: () -> Unit,
    aoSalvar: (nome: String, limite: Money, diaFechamento: Int, diaVencimento: Int) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var limiteTexto by remember { mutableStateOf("") }
    var fechamentoTexto by remember { mutableStateOf("") }
    var vencimentoTexto by remember { mutableStateOf("") }

    val limite = Money.deTexto(limiteTexto)?.takeIf { !it.isNegativo }
    val fechamento = fechamentoTexto.toIntOrNull()?.takeIf { it in 1..31 }
    val vencimento = vencimentoTexto.toIntOrNull()?.takeIf { it in 1..31 }
    val podeSalvar = nome.isNotBlank() && limite != null && fechamento != null && vencimento != null

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Novo cartão") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do cartão") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CampoValor(limiteTexto, { limiteTexto = it }, "Limite (R$)", invalido = limiteTexto.isNotBlank() && limite == null)
                CampoNumero(
                    fechamentoTexto, { fechamentoTexto = it }, "Dia de fechamento (1 a 31)",
                    invalido = fechamentoTexto.isNotBlank() && fechamento == null
                )
                CampoNumero(
                    vencimentoTexto, { vencimentoTexto = it }, "Dia de vencimento (1 a 31)",
                    invalido = vencimentoTexto.isNotBlank() && vencimento == null
                )
                Text(
                    "Compras feitas a partir do dia de fechamento entram na fatura seguinte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = podeSalvar,
                onClick = {
                    if (limite != null && fechamento != null && vencimento != null) {
                        aoSalvar(nome, limite, fechamento, vencimento)
                    }
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoPagarFatura(
    resumo: ResumoCartao,
    contas: List<Conta>,
    aoCancelar: () -> Unit,
    aoPagar: (contaId: Long, valor: Money) -> Unit
) {
    val restante = resumo.faturaAPagar?.restante ?: Money.ZERO
    var contaId by remember { mutableStateOf(contas.firstOrNull()?.id) }
    var valorTexto by remember { mutableStateOf(restante.toBigDecimal().toPlainString().replace('.', ',')) }

    val valor = Money.deTexto(valorTexto)?.takeIf { it.isPositivo }
    val contaEscolhida = contaId

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Pagar fatura — ${resumo.cartao.nome}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Falta pagar: ${restante.formatarBRL()}")
                Seletor(
                    rotulo = "Pagar com",
                    opcoes = contas,
                    selecionado = contas.firstOrNull { it.id == contaId },
                    texto = { it.nome },
                    aoSelecionar = { contaId = it.id }
                )
                CampoValor(valorTexto, { valorTexto = it }, "Valor pago (R$)", invalido = valor == null)
                Text(
                    "O pagamento sai da conta com a data de hoje e reduz a dívida do cartão. " +
                        "Ele não conta como despesa de novo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valor != null && contaEscolhida != null,
                onClick = { if (valor != null && contaEscolhida != null) aoPagar(contaEscolhida, valor) }
            ) { Text("Pagar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}
