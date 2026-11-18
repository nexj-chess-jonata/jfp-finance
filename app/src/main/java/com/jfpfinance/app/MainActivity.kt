package com.jfpfinance.app

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.jfpfinance.app.navigation.JfpNavHost
import com.jfpfinance.app.ui.seguranca.BloqueioScreen
import com.jfpfinance.app.ui.theme.JfpFinanceTheme

// FragmentActivity é exigida pelo BiometricPrompt.
class MainActivity : FragmentActivity() {

    private val dependencias: Dependencias
        get() = (application as JfpFinanceApplication).dependencias

    private var bloqueado by mutableStateOf(false)
    private var saiuEm: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Esconde o conteúdo na tela de aplicativos recentes e impede capturas de tela (seção 21).
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Abre bloqueado sempre que houver PIN, inclusive se o sistema recriar a tela.
        bloqueado = dependencias.pinManager.temPinDefinido()

        setContent {
            JfpFinanceTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (bloqueado) {
                        BloqueioScreen(
                            pinManager = dependencias.pinManager,
                            biometriaAtivada = dependencias.preferencias.biometriaAtivada,
                            aoDesbloquear = { bloqueado = false }
                        )
                    } else {
                        JfpNavHost(dependencias = dependencias)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        saiuEm = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // Bloqueio automático ao voltar depois de um tempo fora do aplicativo.
        val foraPor = SystemClock.elapsedRealtime() - saiuEm
        if (saiuEm != 0L && foraPor >= BLOQUEIO_AUTOMATICO_MS && dependencias.pinManager.temPinDefinido()) {
            bloqueado = true
        }
    }

    private companion object {
        const val BLOQUEIO_AUTOMATICO_MS = 30_000L
    }
}
