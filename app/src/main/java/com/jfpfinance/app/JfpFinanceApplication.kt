package com.jfpfinance.app

import android.app.Application
import android.content.Context
import com.jfpfinance.core.repository.BemRepository
import com.jfpfinance.core.repository.CartaoRepository
import com.jfpfinance.core.repository.CategoriaRepository
import com.jfpfinance.core.repository.ContaRepository
import com.jfpfinance.core.repository.DividaRepository
import com.jfpfinance.core.repository.InvestimentoRepository
import com.jfpfinance.core.repository.MetaRepository
import com.jfpfinance.core.repository.OperacaoRepository
import com.jfpfinance.core.repository.OrcamentoRepository
import com.jfpfinance.core.repository.PessoaRepository
import com.jfpfinance.data.db.JfpDatabase
import com.jfpfinance.data.repository.BemRepositoryImpl
import com.jfpfinance.data.repository.CartaoRepositoryImpl
import com.jfpfinance.data.repository.CategoriaRepositoryImpl
import com.jfpfinance.data.repository.ContaRepositoryImpl
import com.jfpfinance.data.repository.DividaRepositoryImpl
import com.jfpfinance.data.repository.InvestimentoRepositoryImpl
import com.jfpfinance.data.repository.MetaRepositoryImpl
import com.jfpfinance.data.repository.OperacaoRepositoryImpl
import com.jfpfinance.data.repository.OrcamentoRepositoryImpl
import com.jfpfinance.data.repository.PessoaRepositoryImpl
import com.jfpfinance.engine.GestaoFinanceira
import com.jfpfinance.engine.LeitorDeDados
import com.jfpfinance.security.PinManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ponto único onde as dependências do aplicativo são montadas (injeção manual).
 * As telas recebem contratos do core, nunca o banco de dados diretamente.
 */
class JfpFinanceApplication : Application() {

    val dependencias: Dependencias by lazy { Dependencias(this) }

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        escopo.launch { dependencias.categoriaRepository.garantirPadrao() }
    }
}

class Dependencias(context: Context) {
    private val banco: JfpDatabase by lazy { JfpDatabase.obter(context) }

    val contaRepository: ContaRepository by lazy { ContaRepositoryImpl(banco.contaDao()) }
    val operacaoRepository: OperacaoRepository by lazy { OperacaoRepositoryImpl(banco.operacaoDao()) }
    val categoriaRepository: CategoriaRepository by lazy { CategoriaRepositoryImpl(banco.categoriaDao()) }
    val cartaoRepository: CartaoRepository by lazy { CartaoRepositoryImpl(banco.cartaoDao()) }
    val pessoaRepository: PessoaRepository by lazy { PessoaRepositoryImpl(banco.pessoaDao()) }
    val orcamentoRepository: OrcamentoRepository by lazy { OrcamentoRepositoryImpl(banco.orcamentoDao()) }
    val metaRepository: MetaRepository by lazy { MetaRepositoryImpl(banco.metaDao()) }
    val dividaRepository: DividaRepository by lazy { DividaRepositoryImpl(banco.dividaDao()) }
    val bemRepository: BemRepository by lazy { BemRepositoryImpl(banco.bemDao()) }
    val investimentoRepository: InvestimentoRepository by lazy { InvestimentoRepositoryImpl(banco.investimentoDao()) }

    /** Única porta para alterar o livro financeiro. */
    val gestaoFinanceira: GestaoFinanceira by lazy { GestaoFinanceira(operacaoRepository) }

    /** Retrato só de leitura usado pelas análises, pelo assistente e pelos alertas. */
    val leitorDeDados: LeitorDeDados by lazy {
        LeitorDeDados(
            contaRepository, categoriaRepository, cartaoRepository, orcamentoRepository, metaRepository,
            dividaRepository, bemRepository, investimentoRepository, operacaoRepository, pessoaRepository
        )
    }

    val pinManager: PinManager by lazy { PinManager(context) }
    val preferencias: PreferenciasApp by lazy { PreferenciasApp(context) }
}

/** Preferências simples do aplicativo. Não guarda nenhum dado financeiro. */
class PreferenciasApp(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("jfp_config", Context.MODE_PRIVATE)

    var biometriaAtivada: Boolean
        get() = prefs.getBoolean(CHAVE_BIOMETRIA, false)
        set(valor) = prefs.edit().putBoolean(CHAVE_BIOMETRIA, valor).apply()

    var alertasDiarios: Boolean
        get() = prefs.getBoolean(CHAVE_ALERTAS, false)
        set(valor) = prefs.edit().putBoolean(CHAVE_ALERTAS, valor).apply()

    private companion object {
        const val CHAVE_BIOMETRIA = "biometria_ativada"
        const val CHAVE_ALERTAS = "alertas_diarios"
    }
}
