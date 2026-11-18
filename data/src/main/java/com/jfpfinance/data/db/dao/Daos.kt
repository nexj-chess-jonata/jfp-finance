package com.jfpfinance.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jfpfinance.data.db.entity.CartaoEntity
import com.jfpfinance.data.db.entity.CategoriaEntity
import com.jfpfinance.data.db.entity.OperacaoEntity
import com.jfpfinance.data.db.entity.PessoaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OperacaoDao {

    // Ordem de criação: um estorno sempre aparece depois da operação original.
    @Query("SELECT * FROM operacoes ORDER BY criada_em, rowid")
    fun observarTodas(): Flow<List<OperacaoEntity>>

    @Query("SELECT * FROM operacoes ORDER BY criada_em, rowid")
    suspend fun listarTodas(): List<OperacaoEntity>

    // O Room executa a inserção de uma lista dentro de uma única transação: grava tudo ou nada.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun gravar(operacoes: List<OperacaoEntity>)
}

@Dao
interface CategoriaDao {

    @Query("SELECT * FROM categorias WHERE ativa = 1 ORDER BY nome COLLATE NOCASE")
    fun observarAtivas(): Flow<List<CategoriaEntity>>

    @Query("SELECT COUNT(*) FROM categorias")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(categoria: CategoriaEntity): Long

    @Insert
    suspend fun inserirTodas(categorias: List<CategoriaEntity>)

    @Query("UPDATE categorias SET ativa = 0 WHERE id = :id")
    suspend fun arquivar(id: Long)
}

@Dao
interface PessoaDao {

    @Query("SELECT * FROM pessoas WHERE ativa = 1 ORDER BY nome COLLATE NOCASE")
    fun observarAtivas(): Flow<List<PessoaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(pessoa: PessoaEntity): Long

    @Query("UPDATE pessoas SET ativa = 0 WHERE id = :id")
    suspend fun arquivar(id: Long)
}

@Dao
interface CartaoDao {

    @Query("SELECT * FROM cartoes WHERE ativo = 1 ORDER BY nome COLLATE NOCASE")
    fun observarAtivos(): Flow<List<CartaoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(cartao: CartaoEntity): Long

    @Query("UPDATE cartoes SET ativo = 0 WHERE id = :id")
    suspend fun arquivar(id: Long)
}
