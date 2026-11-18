package com.jfpfinance.data.repository

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.AporteMeta
import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Investimento
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.MovimentoInvestimento
import com.jfpfinance.core.model.Orcamento
import com.jfpfinance.core.repository.BemRepository
import com.jfpfinance.core.repository.DividaRepository
import com.jfpfinance.core.repository.InvestimentoRepository
import com.jfpfinance.core.repository.MetaRepository
import com.jfpfinance.core.repository.OrcamentoRepository
import com.jfpfinance.data.db.dao.BemDao
import com.jfpfinance.data.db.dao.DividaDao
import com.jfpfinance.data.db.dao.InvestimentoDao
import com.jfpfinance.data.db.dao.MetaDao
import com.jfpfinance.data.db.dao.OrcamentoDao
import com.jfpfinance.data.db.entity.paraDominio
import com.jfpfinance.data.db.entity.paraEntidade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OrcamentoRepositoryImpl(private val dao: OrcamentoDao) : OrcamentoRepository {
    override fun observarAtivos(): Flow<List<Orcamento>> =
        dao.observarAtivos().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun salvar(orcamento: Orcamento): Long = dao.salvar(orcamento.paraEntidade())
    override suspend fun arquivar(id: Long) = dao.arquivar(id)
}

class MetaRepositoryImpl(private val dao: MetaDao) : MetaRepository {
    override fun observarAtivas(): Flow<List<Meta>> =
        dao.observarAtivas().map { lista -> lista.map { it.paraDominio() } }

    override fun observarAportes(): Flow<List<AporteMeta>> =
        dao.observarAportes().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun salvar(meta: Meta): Long = dao.salvar(meta.paraEntidade())
    override suspend fun arquivar(id: Long) = dao.arquivar(id)
    override suspend fun registrarAporte(aporte: AporteMeta) = dao.inserirAporte(aporte.paraEntidade())
}

class DividaRepositoryImpl(private val dao: DividaDao) : DividaRepository {
    override fun observarAtivas(): Flow<List<Divida>> =
        dao.observarAtivas().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun salvar(divida: Divida): Long = dao.salvar(divida.paraEntidade())
    override suspend fun arquivar(id: Long) = dao.arquivar(id)
}

class BemRepositoryImpl(private val dao: BemDao) : BemRepository {
    override fun observarAtivos(): Flow<List<Bem>> =
        dao.observarAtivos().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun salvar(bem: Bem): Long = dao.salvar(bem.paraEntidade())
    override suspend fun arquivar(id: Long) = dao.arquivar(id)
}

class InvestimentoRepositoryImpl(private val dao: InvestimentoDao) : InvestimentoRepository {
    override fun observarAtivos(): Flow<List<Investimento>> =
        dao.observarAtivos().map { lista -> lista.map { it.paraDominio() } }

    override fun observarMovimentos(): Flow<List<MovimentoInvestimento>> =
        dao.observarMovimentos().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun salvar(investimento: Investimento): Long = dao.salvar(investimento.paraEntidade())
    override suspend fun arquivar(id: Long) = dao.arquivar(id)

    override suspend fun registrarMovimento(movimento: MovimentoInvestimento, novoValorAtual: Money) =
        dao.registrarMovimento(movimento.paraEntidade(), novoValorAtual.centavos)
}
