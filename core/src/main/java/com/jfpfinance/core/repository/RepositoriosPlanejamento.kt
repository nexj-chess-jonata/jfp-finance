package com.jfpfinance.core.repository

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.AporteMeta
import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Investimento
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.MovimentoInvestimento
import com.jfpfinance.core.model.Orcamento
import kotlinx.coroutines.flow.Flow

interface OrcamentoRepository {
    fun observarAtivos(): Flow<List<Orcamento>>
    suspend fun salvar(orcamento: Orcamento): Long
    suspend fun arquivar(id: Long)
}

interface MetaRepository {
    fun observarAtivas(): Flow<List<Meta>>
    fun observarAportes(): Flow<List<AporteMeta>>
    suspend fun salvar(meta: Meta): Long
    suspend fun arquivar(id: Long)
    suspend fun registrarAporte(aporte: AporteMeta)
}

interface DividaRepository {
    fun observarAtivas(): Flow<List<Divida>>
    suspend fun salvar(divida: Divida): Long
    suspend fun arquivar(id: Long)
}

interface BemRepository {
    fun observarAtivos(): Flow<List<Bem>>
    suspend fun salvar(bem: Bem): Long
    suspend fun arquivar(id: Long)
}

interface InvestimentoRepository {
    fun observarAtivos(): Flow<List<Investimento>>
    fun observarMovimentos(): Flow<List<MovimentoInvestimento>>
    suspend fun salvar(investimento: Investimento): Long
    suspend fun arquivar(id: Long)

    /** Grava o movimento e o novo valor atual do investimento juntos, ou nenhum dos dois. */
    suspend fun registrarMovimento(movimento: MovimentoInvestimento, novoValorAtual: Money)
}
