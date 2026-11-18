package com.jfpfinance.core.repository

import com.jfpfinance.core.model.Conta
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acesso às contas. Definido no core e implementado no módulo data,
 * para que regras de negócio e telas não dependam do banco de dados.
 */
interface ContaRepository {
    fun observarContasAtivas(): Flow<List<Conta>>
    suspend fun buscarPorId(id: Long): Conta?
    suspend fun salvar(conta: Conta): Long
    suspend fun arquivar(id: Long)
}
