package com.jfpfinance.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jfpfinance.data.db.entity.AporteMetaEntity
import com.jfpfinance.data.db.entity.BemEntity
import com.jfpfinance.data.db.entity.DividaEntity
import com.jfpfinance.data.db.entity.InvestimentoEntity
import com.jfpfinance.data.db.entity.MetaEntity
import com.jfpfinance.data.db.entity.MovimentoInvestimentoEntity
import com.jfpfinance.data.db.entity.OrcamentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrcamentoDao {
    @Query("SELECT * FROM orcamentos WHERE ativo = 1")
    fun observarAtivos(): Flow<List<OrcamentoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(orcamento: OrcamentoEntity): Long

    @Query("UPDATE orcamentos SET ativo = 0 WHERE id = :id")
    suspend fun arquivar(id: Long)
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM metas WHERE ativa = 1 ORDER BY criada_em")
    fun observarAtivas(): Flow<List<MetaEntity>>

    @Query("SELECT * FROM meta_aportes ORDER BY data_dia, id")
    fun observarAportes(): Flow<List<AporteMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(meta: MetaEntity): Long

    @Query("UPDATE metas SET ativa = 0 WHERE id = :id")
    suspend fun arquivar(id: Long)

    @Insert
    suspend fun inserirAporte(aporte: AporteMetaEntity)
}

@Dao
interface DividaDao {
    @Query("SELECT * FROM dividas WHERE ativa = 1 ORDER BY credor COLLATE NOCASE")
    fun observarAtivas(): Flow<List<DividaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(divida: DividaEntity): Long

    @Query("UPDATE dividas SET ativa = 0 WHERE id = :id")
    suspend fun arquivar(id: Long)
}

@Dao
interface BemDao {
    @Query("SELECT * FROM bens WHERE ativo = 1 ORDER BY nome COLLATE NOCASE")
    fun observarAtivos(): Flow<List<BemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(bem: BemEntity): Long

    @Query("UPDATE bens SET ativo = 0 WHERE id = :id")
    suspend fun arquivar(id: Long)
}

@Dao
abstract class InvestimentoDao {
    @Query("SELECT * FROM investimentos WHERE ativo = 1 ORDER BY nome COLLATE NOCASE")
    abstract fun observarAtivos(): Flow<List<InvestimentoEntity>>

    @Query("SELECT * FROM investimento_movimentos ORDER BY data_dia, id")
    abstract fun observarMovimentos(): Flow<List<MovimentoInvestimentoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun salvar(investimento: InvestimentoEntity): Long

    @Query("UPDATE investimentos SET ativo = 0 WHERE id = :id")
    abstract suspend fun arquivar(id: Long)

    @Insert
    abstract suspend fun inserirMovimento(movimento: MovimentoInvestimentoEntity)

    @Query("UPDATE investimentos SET valor_atual_centavos = :valorAtualCentavos WHERE id = :id")
    abstract suspend fun atualizarValorAtual(id: Long, valorAtualCentavos: Long)

    /** Movimento e novo valor atual gravados juntos, ou nenhum dos dois. */
    @Transaction
    open suspend fun registrarMovimento(movimento: MovimentoInvestimentoEntity, valorAtualCentavos: Long) {
        inserirMovimento(movimento)
        atualizarValorAtual(movimento.investimentoId, valorAtualCentavos)
    }
}
