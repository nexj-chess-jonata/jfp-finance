package com.jfpfinance.app.ui.seguranca

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.jfpfinance.security.BiometricAuthManager
import com.jfpfinance.security.PinManager
import com.jfpfinance.security.ResultadoPin

@Composable
fun BloqueioScreen(
    pinManager: PinManager,
    biometriaAtivada: Boolean,
    aoDesbloquear: () -> Unit
) {
    val activity = LocalContext.current as? FragmentActivity
    val podeUsarBiometria = biometriaAtivada && activity != null &&
        BiometricAuthManager.disponivel(activity)

    var pin by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf<String?>(null) }

    fun pedirBiometria() {
        if (activity != null) {
            BiometricAuthManager.autenticar(
                activity = activity,
                aoSucesso = aoDesbloquear,
                aoFalhar = { erro -> if (erro != null) mensagem = erro }
            )
        }
    }

    fun conferirPin() {
        when (val resultado = pinManager.verificarPin(pin)) {
            ResultadoPin.Correto, ResultadoPin.SemPin -> aoDesbloquear()
            is ResultadoPin.Incorreto -> {
                mensagem = "PIN incorreto. Tentativas restantes: ${resultado.tentativasRestantes}"
            }
            is ResultadoPin.Bloqueado -> {
                mensagem = "Muitas tentativas. Aguarde ${resultado.segundos} segundos."
            }
        }
        pin = ""
    }

    LaunchedEffect(Unit) {
        if (podeUsarBiometria) pedirBiometria()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "JFP FINANCE",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Seu dinheiro. Sua estratégia.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { novo ->
                if (novo.length <= PinManager.TAMANHO_MAXIMO && novo.all { it.isDigit() }) pin = novo
            },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (pin.isNotEmpty()) conferirPin() }),
            isError = mensagem != null,
            supportingText = { Text(mensagem ?: "") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { conferirPin() },
            enabled = pin.length >= PinManager.TAMANHO_MINIMO,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar")
        }
        if (podeUsarBiometria) {
            TextButton(onClick = { pedirBiometria() }) {
                Text("Usar biometria")
            }
        }
    }
}
