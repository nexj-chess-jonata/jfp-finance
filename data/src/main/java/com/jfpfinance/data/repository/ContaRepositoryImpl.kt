package com.jfpfinance.data.repository

import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.repository.ContaRepository
import com.jfpfinance.data.db.dao.ContaDao
import com.jfpfinance.data.db.entity.paraDominio
import com.jfpfinance.data.db.entity.paraEntidade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContaRepositoryImpl(private val dao: ContaDao) : ContaRepository {

    override fun observarContasAtivas(): Flow<List<Conta>> =
        dao.observarAtivas().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun buscarPorId(id: Long): Conta? = dao.buscarPorId(id)?.paraDominio()

    override suspend fun salvar(conta: Conta): Long = dao.salvar(conta.paraEntidade())

    override suspend fun arquivar(id: Long) = dao.arquivar(id)
}
