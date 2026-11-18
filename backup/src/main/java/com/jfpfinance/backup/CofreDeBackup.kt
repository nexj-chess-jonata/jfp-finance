package com.jfpfinance.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupInvalidoException(mensagem: String) : Exception(mensagem)

/** O que dá para saber de um arquivo de backup sem a senha. */
data class CabecalhoDoBackup(val versaoDoBanco: Int, val criadoEm: Long)

data class BackupAberto(val cabecalho: CabecalhoDoBackup, val conteudo: ByteArray)

/**
 * Backup criptografado com senha (seção 31).
 *
 * Formato do arquivo: marca "JFPBKP1", versão do banco, data de criação, parâmetros da chave,
 * e o conteúdo cifrado com AES-256-GCM. A chave vem da senha por PBKDF2-HMAC-SHA256.
 * O GCM autentica o conteúdo e o cabeçalho: qualquer byte alterado, ou a senha errada,
 * faz a abertura falhar em vez de devolver dados corrompidos.
 *
 * Sem a senha não há como recuperar o conteúdo.
 */
object CofreDeBackup {
    private val MARCA = "JFPBKP1\n".toByteArray(Charsets.US_ASCII)
    private const val ITERACOES = 210_000
    private const val TAMANHO_SAL = 16
    private const val TAMANHO_IV = 12
    private const val BITS_DA_ETIQUETA = 128
    private const val TAMANHO_CABECALHO = 8 + 4 + 8 + 4 + TAMANHO_SAL + TAMANHO_IV
    const val SENHA_MINIMA = 8

    fun cifrar(
        conteudo: ByteArray,
        senha: CharArray,
        versaoDoBanco: Int,
        criadoEm: Long,
        aleatorio: SecureRandom = SecureRandom()
    ): ByteArray {
        if (senha.size < SENHA_MINIMA) throw BackupInvalidoException("A senha do backup deve ter pelo menos $SENHA_MINIMA caracteres.")
        val sal = ByteArray(TAMANHO_SAL).also(aleatorio::nextBytes)
        val iv = ByteArray(TAMANHO_IV).also(aleatorio::nextBytes)
        val cabecalho = ByteBuffer.allocate(TAMANHO_CABECALHO)
            .put(MARCA).putInt(versaoDoBanco).putLong(criadoEm).putInt(ITERACOES).put(sal).put(iv)
            .array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, chave(senha, sal, ITERACOES), GCMParameterSpec(BITS_DA_ETIQUETA, iv))
        cipher.updateAAD(cabecalho)
        return cabecalho + cipher.doFinal(conteudo)
    }

    /** Lê o cabeçalho sem precisar da senha. Serve para mostrar a data do backup antes de restaurar. */
    fun inspecionar(arquivo: ByteArray): CabecalhoDoBackup {
        if (arquivo.size < TAMANHO_CABECALHO + BITS_DA_ETIQUETA / 8 ||
            !arquivo.copyOfRange(0, MARCA.size).contentEquals(MARCA)
        ) {
            throw BackupInvalidoException("Este arquivo não é um backup do JFP Finance.")
        }
        val buffer = ByteBuffer.wrap(arquivo, MARCA.size, TAMANHO_CABECALHO - MARCA.size)
        return CabecalhoDoBackup(versaoDoBanco = buffer.getInt(), criadoEm = buffer.getLong())
    }

    fun abrir(arquivo: ByteArray, senha: CharArray): BackupAberto {
        val cabecalho = inspecionar(arquivo)
        val buffer = ByteBuffer.wrap(arquivo, MARCA.size + 12, TAMANHO_CABECALHO - MARCA.size - 12)
        val iteracoes = buffer.getInt()
        if (iteracoes !in 10_000..5_000_000) throw BackupInvalidoException("O arquivo de backup está corrompido.")
        val sal = ByteArray(TAMANHO_SAL).also { buffer.get(it) }
        val iv = ByteArray(TAMANHO_IV).also { buffer.get(it) }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, chave(senha, sal, iteracoes), GCMParameterSpec(BITS_DA_ETIQUETA, iv))
            cipher.updateAAD(arquivo, 0, TAMANHO_CABECALHO)
            BackupAberto(cabecalho, cipher.doFinal(arquivo, TAMANHO_CABECALHO, arquivo.size - TAMANHO_CABECALHO))
        } catch (e: GeneralSecurityException) {
            throw BackupInvalidoException("Senha incorreta ou arquivo de backup corrompido.")
        }
    }

    /** Todo banco SQLite começa com este texto. Confere antes de substituir o banco em uso. */
    fun pareceBancoSqlite(conteudo: ByteArray): Boolean {
        val assinatura = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        return conteudo.size >= 100 && conteudo.copyOfRange(0, assinatura.size).contentEquals(assinatura)
    }

    private fun chave(senha: CharArray, sal: ByteArray, iteracoes: Int): SecretKeySpec {
        val spec = PBEKeySpec(senha, sal, iteracoes, 256)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
