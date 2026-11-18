package com.jfpfinance.core.repository

import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Pessoa
import kotlinx.coroutines.flow.Flow

/**
 * Armazenamento das operações do livro financeiro.
 * Quem decide o que pode ser gravado é o motor (GestaoFinanceira); as telas não gravam direto aqui.
 */
interface OperacaoRepository {
    fun observarTodas(): Flow<List<Operacao>>

    /** Na ordem em que foram criadas (um estorno sempre vem depois do original). */
    suspend fun listarTodas(): List<Operacao>

    /** Grava todas ou nenhuma. Operação com id já existente é substituída. */
    suspend fun gravar(operacoes: List<Operacao>)
}

interface CategoriaRepository {
    fun observarAtivas(): Flow<List<Categoria>>
    suspend fun salvar(categoria: Categoria): Long
    suspend fun arquivar(id: Long)

    /** Cria as categorias padrão se ainda não existir nenhuma. */
    suspend fun garantirPadrao()
}

interface PessoaRepository {
    fun observarAtivas(): Flow<List<Pessoa>>
    suspend fun salvar(pessoa: Pessoa): Long
    suspend fun arquivar(id: Long)
}

interface CartaoRepository {
    fun observarAtivos(): Flow<List<Cartao>>
    suspend fun salvar(cartao: Cartao): Long
    suspend fun arquivar(id: Long)
}
