package com.jfpfinance.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jfpfinance.core.Money
import com.jfpfinance.core.model.TipoConta

private const val VALOR_OCULTO = "R$ ••••••"

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    aoAbrirConfiguracoes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var mostrarNovaConta by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "JFP FINANCE",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Seu dinheiro. Sua estratégia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = viewModel::alternarSaldosOcultos) {
                    Icon(
                        imageVector = if (estado.saldosOcultos) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (estado.saldosOcultos) "Mostrar saldos" else "Ocultar saldos"
                    )
                }
                IconButton(onClick = aoAbrirConfiguracoes) {
                    Icon(Icons.Filled.Settings, contentDescription = "Configurações")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "Saldo total",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (estado.saldosOcultos) VALOR_OCULTO else estado.saldoTotal.formatarBRL(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (estado.contas.size) {
                            0 -> "Nenhuma conta cadastrada"
                            1 -> "1 conta"
                            else -> "${estado.contas.size} contas"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Este mês (confirmado)", style = MaterialTheme.typography.labelLarge)
                    LinhaValor("Receitas", estado.receitasDoMes, estado.saldosOcultos)
                    LinhaValor("Despesas", estado.despesasDoMes, estado.saldosOcultos)
                    LinhaValor("Resultado", estado.resultadoDoMes, estado.saldosOcultos)
                }
            }
        }

        if (estado.vencidos > 0 || estado.proximos > 0) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Contas a pagar e a receber", style = MaterialTheme.typography.labelLarge)
                        if (estado.vencidos > 0) {
                            Text(
                                "Vencidas e pendentes: ${estado.vencidos}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (estado.proximos > 0) {
                            Text(
                                "Nos próximos 7 dias: ${estado.proximos}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LinhaValor("Efeito previsto nas contas", estado.saldoDosProximos, estado.saldosOcultos)
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Saldo por conta",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = { mostrarNovaConta = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  Nova conta")
                }
            }
        }

        if (!estado.carregando && estado.contas.isEmpty()) {
            item {
                Text(
                    text = "Cadastre sua primeira conta para ver o saldo aqui. " +
                        "Depois, registre receitas e despesas na aba Movimentações.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(estado.contas, key = { it.conta.id }) { linha ->
            LinhaConta(linha = linha, oculto = estado.saldosOcultos)
        }
    }

    if (mostrarNovaConta) {
        DialogoNovaConta(
            aoCancelar = { mostrarNovaConta = false },
            aoSalvar = { nome, tipo, saldo ->
                viewModel.adicionarConta(nome, tipo, saldo)
                mostrarNovaConta = false
            }
        )
    }
}

@Composable
private fun LinhaValor(rotulo: String, valor: Money, oculto: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            rotulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (oculto) VALOR_OCULTO else valor.formatarBRL(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (!oculto && valor.isNegativo) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun LinhaConta(linha: SaldoDaContaNaTela, oculto: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(linha.conta.nome, style = MaterialTheme.typography.bodyLarge)
                Text(
                    linha.conta.tipo.rotulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                linha.riscoDeNegativoEm?.let { dia ->
                    Text(
                        "Pode ficar negativa em ${dia.dayOfMonth.toString().padStart(2, '0')}/" +
                            dia.monthValue.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = if (oculto) VALOR_OCULTO else linha.saldo.formatarBRL(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (!oculto && linha.saldo.isNegativo) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun DialogoNovaConta(
    aoCancelar: () -> Unit,
    aoSalvar: (nome: String, tipo: TipoConta, saldo: Money) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var saldoTexto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoConta.CONTA_BANCARIA) }

    // Campo vazio = saldo zero. Texto inválido = não deixa salvar.
    val saldo: Money? = if (saldoTexto.isBlank()) Money.ZERO else Money.deTexto(saldoTexto)
    val podeSalvar = nome.isNotBlank() && saldo != null

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Nova conta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da conta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = saldoTexto,
                    onValueChange = { saldoTexto = it },
                    label = { Text("Saldo inicial (R$)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    isError = saldo == null,
                    supportingText = if (saldo == null) {
                        { Text("Digite um valor como 1.234,56") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    TipoConta.entries.forEach { opcao ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = tipo == opcao,
                                    onClick = { tipo = opcao },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = tipo == opcao, onClick = null)
                            Text(opcao.rotulo, modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = podeSalvar,
                onClick = { if (saldo != null) aoSalvar(nome, tipo, saldo) }
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar") }
        }
    )
}
