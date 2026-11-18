package com.jfpfinance.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jfpfinance.data.db.entity.ContaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContaDao {

    @Query("SELECT * FROM contas WHERE ativa = 1 ORDER BY nome COLLATE NOCASE")
    fun observarAtivas(): Flow<List<ContaEntity>>

    @Query("SELECT * FROM contas WHERE id = :id")
    suspend fun buscarPorId(id: Long): ContaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(conta: ContaEntity): Long

    // Contas nunca são apagadas: são arquivadas, para preservar o histórico.
    @Query("UPDATE contas SET ativa = 0 WHERE id = :id")
    suspend fun arquivar(id: Long)
}
