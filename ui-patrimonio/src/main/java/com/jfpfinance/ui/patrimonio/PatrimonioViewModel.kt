package com.jfpfinance.ui.patrimonio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Investimento
import com.jfpfinance.core.model.MovimentoInvestimento
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.TipoBem
import com.jfpfinance.core.model.TipoInvestimento
import com.jfpfinance.core.model.TipoMovimentoInvestimento
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.core.repository.BemRepository
import com.jfpfinance.core.repository.CartaoRepository
import com.jfpfinance.core.repository.ContaRepository
import com.jfpfinance.core.repository.DividaRepository
import com.jfpfinance.core.repository.InvestimentoRepository
import com.jfpfinance.core.repository.OperacaoRepository
import com.jfpfinance.engine.GestaoFinanceira
import com.jfpfinance.engine.Investimentos
import com.jfpfinance.engine.RegraFinanceiraException
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PatrimonioViewModel(
    contaRepository: ContaRepository,
    cartaoRepository: CartaoRepository,
    dividaRepository: DividaRepository,
    private val bemRepository: BemRepository,
    private val investimentoRepository: InvestimentoRepository,
    operacaoRepository: OperacaoRepository,
    private val gestao: GestaoFinanceira
) : ViewModel() {

    private data class Cadastros(
        val contas: List<Conta>,
        val cartoes: List<Cartao>,
        val dividas: List<Divida>,
        val bens: List<Bem>,
        val investimentos: List<Investimento>
    )

    private val erro = MutableStateFlow<String?>(null)

    private val cadastros = combine(
        contaRepository.observarContasAtivas(),
        cartaoRepository.observarAtivos(),
        dividaRepository.observarAtivas(),
        bemRepository.observarAtivos(),
        investimentoRepository.observarAtivos()
    ) { contas, cartoes, dividas, bens, investimentos ->
        Cadastros(contas, cartoes, dividas, bens, investimentos)
    }

    val estado: StateFlow<PatrimonioUiState> = combine(
        cadastros,
        investimentoRepository.observarMovimentos(),
        operacaoRepository.observarTodas(),
        erro
    ) { c, movimentos, operacoes, mensagem ->
        montarPatrimonio(
            LocalDate.now(), c.contas, c.cartoes, c.dividas, c.bens, c.investimentos, movimentos, operacoes
        ).copy(erro = mensagem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatrimonioUiState())

    fun limparErro() {
        erro.value = null
    }

    // ---------- Bens ----------

    fun novoBem(nome: String, tipo: TipoBem, valorAquisicao: Money, dataAquisicao: LocalDate, valorAtual: Money) =
        executar {
            if (nome.isBlank()) throw RegraFinanceiraException("Dê um nome para o bem.")
            if (valorAquisicao.isNegativo || valorAtual.isNegativo) {
                throw RegraFinanceiraException("Os valores do bem não podem ser negativos.")
            }
            bemRepository.salvar(
                Bem(
                    nome = nome.trim(),
                    tipo = tipo,
                    valorAquisicao = valorAquisicao,
                    dataAquisicao = dataAquisicao,
                    valorAtual = valorAtual
                )
            )
        }

    fun atualizarValorDoBem(bem: Bem, valorAtual: Money) = executar {
        bemRepository.salvar(bem.copy(valorAtual = valorAtual))
    }

    fun arquivarBem(id: Long) = executar { bemRepository.arquivar(id) }

    // ---------- Investimentos ----------

    fun novoInvestimento(nome: String, tipo: TipoInvestimento) = executar {
        if (nome.isBlank()) throw RegraFinanceiraException("Dê um nome para o investimento.")
        investimentoRepository.salvar(Investimento(nome = nome.trim(), tipo = tipo))
    }

    /** Avaliação manual: informa quanto o investimento vale hoje. */
    fun atualizarValorDoInvestimento(investimento: Investimento, valorAtual: Money) = executar {
        investimentoRepository.salvar(investimento.copy(valorAtual = valorAtual))
    }

    fun arquivarInvestimento(id: Long) = executar { investimentoRepository.arquivar(id) }

    /**
     * Registra aporte, resgate ou rendimento. Com [contaId], o dinheiro também sai da conta
     * ou entra nela (primeiro o lançamento na conta, depois o movimento do investimento).
     */
    fun movimentar(
        investimento: Investimento,
        tipo: TipoMovimentoInvestimento,
        valor: Money,
        data: LocalDate,
        quantidade: BigDecimal?,
        contaId: Long?
    ) = executar {
        if (!valor.isPositivo) throw RegraFinanceiraException("O valor deve ser maior que zero.")
        val movimento = if (tipo == TipoMovimentoInvestimento.RESGATE) {
            Investimentos.prepararResgate(investimento, estado.value.movimentos, data, valor, quantidade)
        } else {
            MovimentoInvestimento(
                investimentoId = investimento.id,
                tipo = tipo,
                data = data,
                valor = valor,
                quantidade = if (tipo == TipoMovimentoInvestimento.APORTE) quantidade else null
            )
        }
        if (contaId != null) {
            gestao.registrar(
                Operacao(
                    id = gestao.novoId(),
                    tipo = when (tipo) {
                        TipoMovimentoInvestimento.APORTE -> TipoOperacao.APLICACAO
                        TipoMovimentoInvestimento.RESGATE -> TipoOperacao.RESGATE_INVESTIMENTO
                        TipoMovimentoInvestimento.RENDIMENTO -> TipoOperacao.RECEITA
                    },
                    data = data,
                    valor = valor,
                    contaId = contaId,
                    descricao = "${tipo.rotulo} — ${investimento.nome}"
                )
            )
        }
        investimentoRepository.registrarMovimento(movimento, Investimentos.valorAtualApos(investimento, movimento))
    }

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
            cartaoRepository: CartaoRepository,
            dividaRepository: DividaRepository,
            bemRepository: BemRepository,
            investimentoRepository: InvestimentoRepository,
            operacaoRepository: OperacaoRepository,
            gestao: GestaoFinanceira
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PatrimonioViewModel(
                    contaRepository, cartaoRepository, dividaRepository, bemRepository,
                    investimentoRepository, operacaoRepository, gestao
                )
            }
        }
    }
}
