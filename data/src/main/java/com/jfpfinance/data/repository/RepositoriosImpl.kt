package com.jfpfinance.data.repository

import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.CategoriasPadrao
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Pessoa
import com.jfpfinance.core.repository.CartaoRepository
import com.jfpfinance.core.repository.CategoriaRepository
import com.jfpfinance.core.repository.OperacaoRepository
import com.jfpfinance.core.repository.PessoaRepository
import com.jfpfinance.data.db.dao.CartaoDao
import com.jfpfinance.data.db.dao.CategoriaDao
import com.jfpfinance.data.db.dao.OperacaoDao
import com.jfpfinance.data.db.dao.PessoaDao
import com.jfpfinance.data.db.entity.paraDominio
import com.jfpfinance.data.db.entity.paraEntidade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OperacaoRepositoryImpl(private val dao: OperacaoDao) : OperacaoRepository {

    override fun observarTodas(): Flow<List<Operacao>> =
        dao.observarTodas().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun listarTodas(): List<Operacao> = dao.listarTodas().map { it.paraDominio() }

    override suspend fun gravar(operacoes: List<Operacao>) = dao.gravar(operacoes.map { it.paraEntidade() })
}

class CategoriaRepositoryImpl(private val dao: CategoriaDao) : CategoriaRepository {

    override fun observarAtivas(): Flow<List<Categoria>> =
        dao.observarAtivas().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun salvar(categoria: Categoria): Long = dao.salvar(categoria.paraEntidade())

    override suspend fun arquivar(id: Long) = dao.arquivar(id)

    override suspend fun garantirPadrao() {
        if (dao.contar() == 0) dao.inserirTodas(CategoriasPadrao.lista.map { it.paraEntidade() })
    }
}

class PessoaRepositoryImpl(private val dao: PessoaDao) : PessoaRepository {

    override fun observarAtivas(): Flow<List<Pessoa>> =
        dao.observarAtivas().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun salvar(pessoa: Pessoa): Long = dao.salvar(pessoa.paraEntidade())

    override suspend fun arquivar(id: Long) = dao.arquivar(id)
}

class CartaoRepositoryImpl(private val dao: CartaoDao) : CartaoRepository {

    override fun observarAtivos(): Flow<List<Cartao>> =
        dao.observarAtivos().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun salvar(cartao: Cartao): Long = dao.salvar(cartao.paraEntidade())

    override suspend fun arquivar(id: Long) = dao.arquivar(id)
}
