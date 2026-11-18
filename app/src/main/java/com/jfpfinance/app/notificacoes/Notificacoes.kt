package com.jfpfinance.app.notificacoes

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jfpfinance.app.JfpFinanceApplication
import com.jfpfinance.app.MainActivity
import com.jfpfinance.app.R
import com.jfpfinance.engine.Analises
import com.jfpfinance.engine.Gravidade
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Aviso diário de alertas (seção 26). Opcional e desligado por padrão.
 *
 * Privacidade: a notificação nunca mostra valores, nomes de contas nem o texto dos alertas —
 * só quantos alertas existem. Os detalhes ficam dentro do aplicativo, atrás do PIN.
 */
object Notificacoes {
    private const val CANAL = "alertas_financeiros"
    private const val TRABALHO = "alertas_diarios"
    private const val ID_RESUMO = 1001

    fun permitidas(context: Context): Boolean {
        val concedida = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return concedida && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Agenda a verificação uma vez por dia. Chamar de novo não cria um segundo agendamento. */
    fun agendar(context: Context) {
        criarCanal(context)
        val pedido = PeriodicWorkRequestBuilder<AlertasWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TRABALHO, ExistingPeriodicWorkPolicy.KEEP, pedido)
    }

    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TRABALHO)
        NotificationManagerCompat.from(context).cancel(ID_RESUMO)
    }

    private fun criarCanal(context: Context) {
        val canal = NotificationChannel(CANAL, "Alertas financeiros", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Aviso diário quando há alertas importantes"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    fun mostrarResumo(context: Context, quantidade: Int) {
        if (!permitidas(context)) return
        criarCanal(context)
        val abrir = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val texto = if (quantidade == 1) {
            "Há 1 alerta importante. Abra o aplicativo para ver."
        } else {
            "Há $quantidade alertas importantes. Abra o aplicativo para ver."
        }
        val notificacao = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("JFP Finance")
            .setContentText(texto)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        try {
            // Mesmo ID sempre: uma notificação nova substitui a anterior em vez de acumular.
            NotificationManagerCompat.from(context).notify(ID_RESUMO, notificacao)
        } catch (e: SecurityException) {
            // Permissão retirada entre a checagem e o envio: não notifica.
        }
    }
}

/** Verificação diária em segundo plano: calcula os alertas localmente e avisa se houver importantes. */
class AlertasWorker(context: Context, parametros: WorkerParameters) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val aplicativo = applicationContext as? JfpFinanceApplication ?: return Result.success()
        val dependencias = aplicativo.dependencias
        if (!dependencias.preferencias.alertasDiarios) return Result.success()

        val dados = dependencias.leitorDeDados.observar().first()
        val importantes = Analises.diagnostico(dados).alertas.count { it.gravidade != Gravidade.INFO }
        if (importantes > 0) Notificacoes.mostrarResumo(applicationContext, importantes)
        return Result.success()
    }
}
