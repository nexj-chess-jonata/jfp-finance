package com.jfpfinance.app.ui.dados

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import com.jfpfinance.app.notificacoes.Notificacoes
import com.jfpfinance.backup.BackupInvalidoException
import com.jfpfinance.backup.CabecalhoDoBackup
import com.jfpfinance.backup.CofreDeBackup
import com.jfpfinance.data.db.JfpDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArquivoLido(val nome: String, val bytes: ByteArray)

/**
 * Operações com arquivos e com o banco inteiro: ler extrato, salvar relatório,
 * criar e restaurar backup, apagar todos os dados.
 */
class GerenciadorDeDados(context: Context) {
    private val context = context.applicationContext

    suspend fun ler(uri: Uri): ArquivoLido = withContext(Dispatchers.IO) {
        val nome = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: "arquivo"
        val bytes = context.contentResolver.openInputStream(uri)?.use { entrada ->
            val saida = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val lidos = entrada.read(buffer)
                if (lidos < 0) break
                saida.write(buffer, 0, lidos)
                if (saida.size() > TAMANHO_MAXIMO) throw IOException("O arquivo é grande demais (mais de 50 MB).")
            }
            saida.toByteArray()
        } ?: throw IOException("Não foi possível abrir o arquivo.")
        ArquivoLido(nome, bytes)
    }

    suspend fun escrever(uri: Uri, bytes: ByteArray) = withContext(Dispatchers.IO) {
        // "wt" trunca o arquivo: se o destino já existia e era maior, não sobra lixo no final.
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: throw IOException("Não foi possível gravar o arquivo.")
    }

    /** Gera um PDF simples (A4, texto corrido) a partir das linhas do relatório. */
    suspend fun pdf(linhas: List<String>): ByteArray = withContext(Dispatchers.Default) {
        val largura = 595
        val altura = 842
        val margem = 40f
        val normal = Paint().apply { textSize = 10f; isAntiAlias = true }
        val titulo = Paint().apply { textSize = 14f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }
        val documento = PdfDocument()
        try {
            var numero = 1
            var pagina = documento.startPage(PdfDocument.PageInfo.Builder(largura, altura, numero).create())
            var y = margem + 14f
            linhas.forEachIndexed { indice, linha ->
                val pincel = if (indice == 0) titulo else normal
                val passo = if (indice == 0) 22f else 14f
                var resto = linha.ifEmpty { " " }
                while (resto.isNotEmpty()) {
                    if (y > altura - margem) {
                        documento.finishPage(pagina)
                        numero++
                        pagina = documento.startPage(PdfDocument.PageInfo.Builder(largura, altura, numero).create())
                        y = margem + 14f
                    }
                    val cabem = pincel.breakText(resto, true, largura - 2 * margem, null).coerceAtLeast(1)
                    pagina.canvas.drawText(resto, 0, cabem, margem, y, pincel)
                    resto = resto.substring(cabem)
                    y += passo
                }
            }
            documento.finishPage(pagina)
            ByteArrayOutputStream().also { documento.writeTo(it) }.toByteArray()
        } finally {
            documento.close()
        }
    }

    /**
     * Grava uma cópia cifrada do banco em [destino] e a relê para conferir:
     * só termina sem erro se a cópia gravada abre com a senha e é idêntica ao banco.
     */
    suspend fun criarBackup(destino: Uri, senha: CharArray) = withContext(Dispatchers.IO) {
        JfpDatabase.consolidar(context)
        val banco = JfpDatabase.arquivo(context).readBytes()
        escrever(destino, CofreDeBackup.cifrar(banco, senha, JfpDatabase.VERSAO, System.currentTimeMillis()))
        val conferido = CofreDeBackup.abrir(ler(destino).bytes, senha)
        if (!conferido.conteudo.contentEquals(banco)) {
            throw BackupInvalidoException("O backup foi gravado, mas a conferência falhou. Crie o backup de novo.")
        }
    }

    suspend fun inspecionarBackup(origem: Uri): CabecalhoDoBackup = CofreDeBackup.inspecionar(ler(origem).bytes)

    /**
     * Substitui o banco atual pelo do backup. O banco atual fica guardado ao lado, como
     * cópia de segurança, até a próxima restauração. Depois disso o aplicativo deve ser reiniciado.
     */
    suspend fun restaurar(origem: Uri, senha: CharArray) = withContext(Dispatchers.IO) {
        val aberto = CofreDeBackup.abrir(ler(origem).bytes, senha)
        if (!CofreDeBackup.pareceBancoSqlite(aberto.conteudo)) {
            throw BackupInvalidoException("O conteúdo do backup não é um banco de dados válido.")
        }
        if (aberto.cabecalho.versaoDoBanco > JfpDatabase.VERSAO) {
            throw BackupInvalidoException("Este backup foi criado por uma versão mais nova do aplicativo. Atualize o aplicativo antes de restaurar.")
        }
        val banco = JfpDatabase.arquivo(context)
        val novo = File(banco.parentFile, banco.name + ".restaurando")
        novo.writeBytes(aberto.conteudo)

        JfpDatabase.consolidar(context)
        JfpDatabase.fechar()
        val anterior = File(banco.parentFile, banco.name + NOME_DA_COPIA_ANTERIOR)
        anterior.delete()
        if (banco.exists() && !banco.renameTo(anterior)) throw IOException("Não foi possível guardar o banco atual.")
        if (!novo.renameTo(banco)) {
            anterior.renameTo(banco)
            throw IOException("Não foi possível colocar o backup no lugar. O banco anterior foi mantido.")
        }
        apagarAuxiliares(banco)
    }

    /** Remove do aparelho o banco, a cópia anterior, o PIN e as preferências. */
    suspend fun apagarTudo() = withContext(Dispatchers.IO) {
        Notificacoes.cancelar(context)
        JfpDatabase.fechar()
        val banco = JfpDatabase.arquivo(context)
        File(banco.parentFile, banco.name + NOME_DA_COPIA_ANTERIOR).delete()
        context.deleteDatabase(banco.name)
        apagarAuxiliares(banco)
        listOf("jfp_config", "jfp_seguranca").forEach { nome ->
            context.getSharedPreferences(nome, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    private fun apagarAuxiliares(banco: File) {
        listOf("-wal", "-shm", "-journal").forEach { sufixo -> File(banco.parentFile, banco.name + sufixo).delete() }
    }

    /** Fecha e abre o aplicativo de novo, para ele recomeçar com o banco que está no disco. */
    fun reiniciar() {
        val abrir = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (abrir != null) {
            abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(abrir)
        }
        Runtime.getRuntime().exit(0)
    }

    private companion object {
        const val TAMANHO_MAXIMO = 50 * 1024 * 1024
        const val NOME_DA_COPIA_ANTERIOR = ".antes-da-restauracao"
    }
}
