package com.jfpfinance.ui.assistente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jfpfinance.engine.Cenario
import com.jfpfinance.engine.LeitorDeDados
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * O assistente só lê: este ViewModel recebe apenas o [LeitorDeDados], que não tem
 * nenhum método de gravação. A conversa e as projeções ficam só na memória.
 */
class AssistenteViewModel(leitor: LeitorDeDados) : ViewModel() {

    private val conversa = MutableStateFlow<List<Mensagem>>(emptyList())
    private val configuracao = MutableStateFlow(ConfiguracaoDaProjecao())

    val estado: StateFlow<AssistenteUiState> =
        combine(leitor.observar(), conversa, configuracao) { dados, mensagens, config ->
            montarAssistente(dados, mensagens, config)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssistenteUiState())

    fun perguntar(pergunta: String) {
        val dados = estado.value.dados ?: return
        if (pergunta.isBlank()) return
        conversa.value = conversaComResposta(conversa.value, pergunta, dados)
    }

    fun limparConversa() {
        conversa.value = emptyList()
    }

    fun escolherHorizonte(horizonte: Horizonte) {
        configuracao.value = configuracao.value.copy(horizonte = horizonte)
    }

    fun escolherCenario(cenario: Cenario) {
        configuracao.value = configuracao.value.copy(cenario = cenario)
    }

    companion object {
        fun fabrica(leitor: LeitorDeDados): ViewModelProvider.Factory = viewModelFactory {
            initializer { AssistenteViewModel(leitor) }
        }
    }
}
