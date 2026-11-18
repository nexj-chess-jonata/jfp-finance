package com.jfpfinance.app.ui.seguranca

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jfpfinance.app.PreferenciasApp
import com.jfpfinance.app.notificacoes.Notificacoes
import com.jfpfinance.security.BiometricAuthManager
import com.jfpfinance.security.PinManager
import com.jfpfinance.security.ResultadoPin

@Composable
fun ConfiguracoesScreen(
    pinManager: PinManager,
    preferencias: PreferenciasApp,
    aoAbrirDados: () -> Unit,
    aoVoltar: () -> Unit
) {
    val context = LocalContext.current
    val biometriaDisponivel = remember { BiometricAuthManager.disponivel(context) }

    var temPin by remember { mutableStateOf(pinManager.temPinDefinido()) }
    var biometriaAtivada by remember { mutableStateOf(preferencias.biometriaAtivada) }
    var mostrarDialogoPin by remember { mutableStateOf(false) }
    var alertasDiarios by remember { mutableStateOf(preferencias.alertasDiarios && Notificacoes.permitidas(context)) }
    var avisoDeNotificacao by remember { mutableStateOf<String?>(null) }

    fun ligarAlertas() {
        preferencias.alertasDiarios = true
        alertasDiarios = true
        avisoDeNotificacao = null
        Notificacoes.agendar(context)
    }

    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida && Notificacoes.permitidas(context)) {
            ligarAlertas()
        } else {
            avisoDeNotificacao = "As notificações estão bloqueadas para este aplicativo nas configurações do Android."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text("Configurações", style = MaterialTheme.typography.titleLarge)
        }

        Text(
            "Segurança",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PIN de acesso", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (temPin) {
                        "O aplicativo pede o PIN ao abrir e depois de 30 segundos fora dele."
                    } else {
                        "Sem PIN, qualquer pessoa com o aparelho desbloqueado abre o aplicativo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { mostrarDialogoPin = true }) {
                    Text(if (temPin) "Alterar PIN" else "Definir PIN")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Desbloqueio por biometria", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when {
                            !biometriaDisponivel -> "Este aparelho não tem biometria configurada."
                            !temPin -> "Defina um PIN primeiro."
                            else -> "Digital ou rosto. O PIN continua valendo como alternativa."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = biometriaAtivada && temPin && biometriaDisponivel,
                    enabled = temPin && biometriaDisponivel,
                    onCheckedChange = { ativar ->
                        biometriaAtivada = ativar
                        preferencias.biometriaAtivada = ativar
                    }
                )
            }
        }

        Text(
            "Dados",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Importar, relatórios, backup e família", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Importar extrato do banco, gerar relatórios, criar ou restaurar backup, " +
                        "cadastrar pessoas da casa e apagar todos os dados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = aoAbrirDados) { Text("Abrir") }
            }
        }

        Text(
            "Alertas",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Aviso diário de alertas", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        avisoDeNotificacao
                            ?: "Uma notificação por dia quando houver alertas importantes. " +
                            "Ela mostra só a quantidade, nunca valores.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (avisoDeNotificacao != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Switch(
                    checked = alertasDiarios,
                    onCheckedChange = { ligar ->
                        when {
                            !ligar -> {
                                preferencias.alertasDiarios = false
                                alertasDiarios = false
                                Notificacoes.cancelar(context)
                            }
                            Notificacoes.permitidas(context) -> {
                                ligarAlertas()
                            }
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                                pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            else -> {
                                avisoDeNotificacao =
                                    "As notificações estão bloqueadas para este aplicativo nas configurações do Android."
                            }
                        }
                    }
                )
            }
        }

        Text(
            "Seus dados ficam somente neste aparelho. Nada é enviado para a internet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (mostrarDialogoPin) {
        DialogoPin(
            pinManager = pinManager,
            exigirPinAtual = temPin,
            aoCancelar = { mostrarDialogoPin = false },
            aoConcluir = {
                temPin = true
                mostrarDialogoPin = false
            }
        )
    }
}

@Composable
private fun DialogoPin(
    pinManager: PinManager,
    exigirPinAtual: Boolean,
    aoCancelar: () -> Unit,
    aoConcluir: () -> Unit
) {
    var atual by remember { mutableStateOf("") }
    var novo by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }

    fun salvar() {
        if (!PinManager.pinValido(novo)) {
            erro = "O PIN deve ter de ${PinManager.TAMANHO_MINIMO} a ${PinManager.TAMANHO_MAXIMO} dígitos."
            return
        }
        if (novo != confirmacao) {
            erro = "A confirmação não é igual ao novo PIN."
            return
        }
        if (exigirPinAtual) {
            when (val resultado = pinManager.verificarPin(atual)) {
                ResultadoPin.Correto, ResultadoPin.SemPin -> Unit
                is ResultadoPin.Incorreto -> {
                    erro = "PIN atual incorreto. Tentativas restantes: ${resultado.tentativasRestantes}"
                    return
                }
                is ResultadoPin.Bloqueado -> {
                    erro = "Muitas tentativas. Aguarde ${resultado.segundos} segundos."
                    return
                }
            }
        }
        pinManager.definirPin(novo)
        aoConcluir()
    }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(if (exigirPinAtual) "Alterar PIN" else "Definir PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (exigirPinAtual) {
                    CampoPin(valor = atual, rotulo = "PIN atual", aoMudar = { atual = it })
                }
                CampoPin(valor = novo, rotulo = "Novo PIN", aoMudar = { novo = it })
                CampoPin(valor = confirmacao, rotulo = "Confirmar novo PIN", aoMudar = { confirmacao = it })
                Text(
                    erro ?: "Não existe recuperação de PIN nesta fase. Guarde-o em local seguro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (erro != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = { salvar() }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun CampoPin(valor: String, rotulo: String, aoMudar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { texto ->
            if (texto.length <= PinManager.TAMANHO_MAXIMO && texto.all { it.isDigit() }) aoMudar(texto)
        },
        label = { Text(rotulo) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}
