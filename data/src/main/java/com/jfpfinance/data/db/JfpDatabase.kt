package com.jfpfinance.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jfpfinance.data.db.dao.BemDao
import com.jfpfinance.data.db.dao.CartaoDao
import com.jfpfinance.data.db.dao.CategoriaDao
import com.jfpfinance.data.db.dao.ContaDao
import com.jfpfinance.data.db.dao.DividaDao
import com.jfpfinance.data.db.dao.InvestimentoDao
import com.jfpfinance.data.db.dao.MetaDao
import com.jfpfinance.data.db.dao.OperacaoDao
import com.jfpfinance.data.db.dao.OrcamentoDao
import com.jfpfinance.data.db.dao.PessoaDao
import com.jfpfinance.data.db.entity.AporteMetaEntity
import com.jfpfinance.data.db.entity.BemEntity
import com.jfpfinance.data.db.entity.CartaoEntity
import com.jfpfinance.data.db.entity.CategoriaEntity
import com.jfpfinance.data.db.entity.ContaEntity
import com.jfpfinance.data.db.entity.DividaEntity
import com.jfpfinance.data.db.entity.InvestimentoEntity
import com.jfpfinance.data.db.entity.MetaEntity
import com.jfpfinance.data.db.entity.MovimentoInvestimentoEntity
import com.jfpfinance.data.db.entity.OperacaoEntity
import com.jfpfinance.data.db.entity.OrcamentoEntity
import com.jfpfinance.data.db.entity.PessoaEntity
import java.io.File

/**
 * Banco local do JFP Finance (local-first, seção 22).
 *
 * Continua na versão 1 porque nenhum APK foi distribuído ainda: não existe banco antigo
 * para migrar. A partir do primeiro APK instalado, toda mudança de esquema deve subir a
 * versão e registrar uma Migration — nunca usar migração destrutiva, porque isso apagaria
 * os dados financeiros do usuário.
 */
@Database(
    entities = [
        ContaEntity::class,
        OperacaoEntity::class,
        CategoriaEntity::class,
        CartaoEntity::class,
        OrcamentoEntity::class,
        MetaEntity::class,
        AporteMetaEntity::class,
        DividaEntity::class,
        BemEntity::class,
        InvestimentoEntity::class,
        MovimentoInvestimentoEntity::class,
        PessoaEntity::class
    ],
    version = JfpDatabase.VERSAO,
    exportSchema = true
)
abstract class JfpDatabase : RoomDatabase() {

    abstract fun contaDao(): ContaDao
    abstract fun operacaoDao(): OperacaoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun cartaoDao(): CartaoDao
    abstract fun orcamentoDao(): OrcamentoDao
    abstract fun metaDao(): MetaDao
    abstract fun dividaDao(): DividaDao
    abstract fun bemDao(): BemDao
    abstract fun investimentoDao(): InvestimentoDao
    abstract fun pessoaDao(): PessoaDao

    companion object {
        const val VERSAO = 1
        private const val NOME = "jfp_finance.db"

        fun arquivo(context: Context): File = context.applicationContext.getDatabasePath(NOME)

        /**
         * Passa para o arquivo principal tudo o que ainda está no arquivo de registro do SQLite (WAL).
         * Chamar antes de copiar o banco para um backup, para a cópia ficar completa.
         */
        fun consolidar(context: Context) {
            obter(context).openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        }

        /** Fecha o banco. Depois disso o aplicativo precisa ser reiniciado antes de usar os dados de novo. */
        fun fechar() {
            synchronized(this) {
                instancia?.close()
                instancia = null
            }
        }

        @Volatile
        private var instancia: JfpDatabase? = null

        fun obter(context: Context): JfpDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    JfpDatabase::class.java,
                    NOME
                ).build().also { instancia = it }
            }
    }
}
