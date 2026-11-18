package com.jfpfinance.ui.comum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jfpfinance.core.Money

/** Componentes de interface usados por mais de uma área do aplicativo. */

/** Linha de abas em forma de botões. */
@Composable
fun Abas(titulos: List<String>, selecionada: Int, aoSelecionar: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        titulos.forEachIndexed { indice, titulo ->
            if (selecionada == indice) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            } else {
                OutlinedButton(
                    onClick = { aoSelecionar(indice) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) { Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** Botão que abre uma lista de opções. */
@Composable
fun <T> Seletor(
    rotulo: String,
    opcoes: List<T>,
    selecionado: T?,
    texto: (T) -> String,
    aoSelecionar: (T) -> Unit,
    textoVazio: String = "selecionar"
) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { aberto = true },
            enabled = opcoes.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$rotulo: ${selecionado?.let(texto) ?: textoVazio}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(texto(opcao)) },
                    onClick = {
                        aoSelecionar(opcao)
                        aberto = false
                    }
                )
            }
        }
    }
}

@Composable
fun CampoValor(valor: String, aoMudar: (String) -> Unit, rotulo: String, invalido: Boolean = false) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        placeholder = { Text("0,00") },
        singleLine = true,
        isError = invalido,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Campo de número inteiro com até três dígitos. */
@Composable
fun CampoNumero(valor: String, aoMudar: (String) -> Unit, rotulo: String, invalido: Boolean = false) {
    OutlinedTextField(
        value = valor,
        onValueChange = { texto -> if (texto.length <= 3 && texto.all { it.isDigit() }) aoMudar(texto) },
        label = { Text(rotulo) },
        singleLine = true,
        isError = invalido,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun CampoTexto(valor: String, aoMudar: (String) -> Unit, rotulo: String, invalido: Boolean = false) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        isError = invalido,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Rótulo à esquerda e valor em reais à direita. Valores negativos aparecem em vermelho. */
@Composable
fun LinhaValor(rotulo: String, valor: Money, destaque: Boolean = false) {
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
fun TextoDeApoio(texto: String, erro: Boolean = false) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = if (erro) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun DialogoDeErro(mensagem: String, aoFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Não foi possível concluir") },
        text = { Text(mensagem) },
        confirmButton = { TextButton(onClick = aoFechar) { Text("OK") } }
    )
}

/** Pergunta de confirmação para ações que removem algo da tela. */
@Composable
fun DialogoDeConfirmacao(titulo: String, mensagem: String, aoConfirmar: () -> Unit, aoCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = { Text(mensagem) },
        confirmButton = { TextButton(onClick = aoConfirmar) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}
