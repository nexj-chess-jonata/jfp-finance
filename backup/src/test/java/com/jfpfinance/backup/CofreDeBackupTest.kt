package com.jfpfinance.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CofreDeBackupTest {

    private val senha = "senha-bem-longa".toCharArray()
    private val conteudo = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII) + ByteArray(5000) { (it % 251).toByte() }

    private fun cifrado() = CofreDeBackup.cifrar(conteudo, senha, versaoDoBanco = 1, criadoEm = 1_795_000_000_000)

    @Test
    fun abreComASenhaCertaEDevolveOConteudoIdentico() {
        val aberto = CofreDeBackup.abrir(cifrado(), senha)
        assertArrayEquals(conteudo, aberto.conteudo)
        assertEquals(1, aberto.cabecalho.versaoDoBanco)
        assertEquals(1_795_000_000_000, aberto.cabecalho.criadoEm)
        assertTrue(CofreDeBackup.pareceBancoSqlite(aberto.conteudo))
    }

    @Test
    fun oArquivoNaoContemOConteudoEmClaro() {
        val arquivo = cifrado()
        val trecho = conteudo.copyOfRange(0, 15)
        val achou = (0..arquivo.size - trecho.size).any { i -> arquivo.copyOfRange(i, i + trecho.size).contentEquals(trecho) }
        assertFalse(achou)
        // Dois backups do mesmo conteúdo são diferentes (sal e IV novos a cada vez).
        assertFalse(arquivo.contentEquals(cifrado()))
    }

    @Test
    fun senhaErradaERecusada() {
        val erro = assertThrows(BackupInvalidoException::class.java) {
            CofreDeBackup.abrir(cifrado(), "outra-senha-longa".toCharArray())
        }
        assertTrue(erro.message!!.contains("Senha incorreta"))
    }

    @Test
    fun qualquerAlteracaoNoArquivoERecusada() {
        val arquivo = cifrado()
        // No conteúdo cifrado, no cabeçalho (versão do banco) e no fim (etiqueta de autenticação).
        listOf(arquivo.size / 2, 9, arquivo.size - 1).forEach { posicao ->
            val adulterado = arquivo.copyOf().also { it[posicao] = (it[posicao] + 1).toByte() }
            assertThrows(BackupInvalidoException::class.java) { CofreDeBackup.abrir(adulterado, senha) }
        }
        assertThrows(BackupInvalidoException::class.java) {
            CofreDeBackup.abrir(arquivo.copyOfRange(0, arquivo.size - 100), senha)
        }
    }

    @Test
    fun arquivoQueNaoEBackupERecusado() {
        assertThrows(BackupInvalidoException::class.java) { CofreDeBackup.inspecionar(ByteArray(10)) }
        assertThrows(BackupInvalidoException::class.java) { CofreDeBackup.abrir(ByteArray(500) { 7 }, senha) }
        assertFalse(CofreDeBackup.pareceBancoSqlite(ByteArray(200)))
    }

    @Test
    fun cabecalhoPodeSerLidoSemSenha() {
        assertEquals(CabecalhoDoBackup(1, 1_795_000_000_000), CofreDeBackup.inspecionar(cifrado()))
    }

    @Test
    fun senhaCurtaNaoEAceita() {
        assertThrows(BackupInvalidoException::class.java) {
            CofreDeBackup.cifrar(conteudo, "1234567".toCharArray(), 1, 1)
        }
    }
}
