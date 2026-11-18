package com.jfpfinance.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.TipoConta
import com.jfpfinance.core.repository.ContaRepository
import com.jfpfinance.core.repository.OperacaoRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val contaRepository: ContaRepository,
    operacaoRepository: OperacaoRepository
) : ViewModel() {

    private val saldosOcultos = MutableStateFlow(false)

    val estado: StateFlow<DashboardUiState> =
        combine(
            contaRepository.observarContasAtivas(),
            operacaoRepository.observarTodas(),
            saldosOcultos
        ) { contas, operacoes, ocultos ->
            montarDashboard(LocalDate.now(), contas, operacoes).copy(saldosOcultos = ocultos)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun alternarSaldosOcultos() {
        saldosOcultos.value = !saldosOcultos.value
    }

    fun adicionarConta(nome: String, tipo: TipoConta, saldoInicial: Money) {
        viewModelScope.launch {
            contaRepository.salvar(Conta(nome = nome.trim(), tipo = tipo, saldoInicial = saldoInicial))
        }
    }

    companion object {
        fun fabrica(
            contaRepository: ContaRepository,
            operacaoRepository: OperacaoRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { DashboardViewModel(contaRepository, operacaoRepository) }
        }
    }
}
