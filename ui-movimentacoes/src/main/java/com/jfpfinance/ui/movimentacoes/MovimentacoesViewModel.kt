package com.jfpfinance.ui.movimentacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.repository.CartaoRepository
import com.jfpfinance.core.repository.CategoriaRepository
import com.jfpfinance.core.repository.ContaRepository
import com.jfpfinance.core.repository.OperacaoRepository
import com.jfpfinance.core.repository.PessoaRepository
import com.jfpfinance.engine.GestaoFinanceira
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MovimentacoesViewModel(
    contaRepository: ContaRepository,
    categoriaRepository: CategoriaRepository,
    private val cartaoRepository: CartaoRepository,
    operacaoRepository: OperacaoRepository,
    pessoaRepository: PessoaRepository,
    private val gestao: GestaoFinanceira
) : ViewModel() {

    private val mes = MutableStateFlow(YearMonth.now())
    private val erro = MutableStateFlow<String?>(null)

    private val dados = combine(
        contaRepository.observarContasAtivas(),
        categoriaRepository.observarAtivas(),
        cartaoRepository.observarAtivos(),
        operacaoRepository.observarTodas(),
        mes
    ) { contas, categorias, cartoes, operacoes, mesAtual ->
        montarEstado(LocalDate.now(), mesAtual, contas, categorias, cartoes, operacoes)
    }

    val estado: StateFlow<MovimentacoesUiState> =
        combine(dados, pessoaRepository.observarAtivas(), erro) { estadoAtual, pessoas, mensagem ->
            estadoAtual.copy(pessoas = pessoas, erro = mensagem)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovimentacoesUiState())

    fun mesAnterior() {
        mes.value = mes.value.minusMonths(1)
    }

    fun proximoMes() {
        mes.value = mes.value.plusMonths(1)
    }

    fun limparErro() {
        erro.value = null
    }

    fun lancar(novo: NovoLancamento) = executar {
        val operacao = novo.paraOperacao(gestao.novoId(), System.currentTimeMillis())
        when {
            novo.vezes <= 1 || novo.tipo == TipoLancamento.TRANSFERENCIA -> gestao.registrar(operacao)
            novo.tipo == TipoLancamento.COMPRA_CARTAO -> gestao.registrarParcelado(operacao, novo.vezes)
            else -> gestao.registrarRepetindo(operacao, novo.vezes)
        }
    }

    /** O estorno leva a data de hoje, ou a data da própria operação quando ela é futura (parcela). */
    fun estornar(operacao: Operacao) = executar {
        gestao.estornar(operacao.id, maxOf(LocalDate.now(), operacao.data))
    }

    fun confirmarHoje(id: String) = executar { gestao.confirmar(id, LocalDate.now()) }

    fun cancelarPrevista(id: String) = executar { gestao.cancelarPrevista(id) }

    fun salvarCartao(nome: String, limite: Money, diaFechamento: Int, diaVencimento: Int) = executar {
        cartaoRepository.salvar(
            Cartao(nome = nome.trim(), limite = limite, diaFechamento = diaFechamento, diaVencimento = diaVencimento)
        )
    }

    fun pagarFatura(cartaoId: Long, contaId: Long, valor: Money) = executar {
        gestao.pagarFatura(cartaoId, contaId, valor, LocalDate.now())
    }

    /** Erros de regra financeira viram mensagem na tela; nada é gravado quando a regra falha. */
    private fun executar(acao: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                acao()
            } catch (e: IllegalArgumentException) {
                erro.value = e.message ?: "Não foi possível concluir a operação."
            }
        }
    }

    companion object {
        fun fabrica(
            contaRepository: ContaRepository,
            categoriaRepository: CategoriaRepository,
            cartaoRepository: CartaoRepository,
            operacaoRepository: OperacaoRepository,
            pessoaRepository: PessoaRepository,
            gestao: GestaoFinanceira
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MovimentacoesViewModel(
                    contaRepository, categoriaRepository, cartaoRepository, operacaoRepository, pessoaRepository, gestao
                )
            }
        }
    }
}
