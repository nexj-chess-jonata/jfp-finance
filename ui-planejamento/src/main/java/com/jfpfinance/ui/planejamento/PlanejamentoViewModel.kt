package com.jfpfinance.ui.planejamento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jfpfinance.core.Money
import com.jfpfinance.core.model.AporteMeta
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.Orcamento
import com.jfpfinance.core.model.SistemaAmortizacao
import com.jfpfinance.core.model.TipoMeta
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.core.repository.CategoriaRepository
import com.jfpfinance.core.repository.ContaRepository
import com.jfpfinance.core.repository.DividaRepository
import com.jfpfinance.core.repository.MetaRepository
import com.jfpfinance.core.repository.OperacaoRepository
import com.jfpfinance.core.repository.OrcamentoRepository
import com.jfpfinance.engine.Amortizacao
import com.jfpfinance.engine.GestaoFinanceira
import com.jfpfinance.engine.Metas
import com.jfpfinance.engine.ProgressoDaMeta
import com.jfpfinance.engine.RegraFinanceiraException
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanejamentoViewModel(
    contaRepository: ContaRepository,
    categoriaRepository: CategoriaRepository,
    private val orcamentoRepository: OrcamentoRepository,
    private val metaRepository: MetaRepository,
    private val dividaRepository: DividaRepository,
    operacaoRepository: OperacaoRepository,
    private val gestao: GestaoFinanceira
) : ViewModel() {

    private data class Cadastros(
        val contas: List<Conta>,
        val categorias: List<Categoria>,
        val orcamentos: List<Orcamento>,
        val metas: List<Meta>,
        val aportes: List<AporteMeta>
    )

    private val erro = MutableStateFlow<String?>(null)

    private val cadastros = combine(
        contaRepository.observarContasAtivas(),
        categoriaRepository.observarAtivas(),
        orcamentoRepository.observarAtivos(),
        metaRepository.observarAtivas(),
        metaRepository.observarAportes()
    ) { contas, categorias, orcamentos, metas, aportes ->
        Cadastros(contas, categorias, orcamentos, metas, aportes)
    }

    val estado: StateFlow<PlanejamentoUiState> = combine(
        cadastros,
        dividaRepository.observarAtivas(),
        operacaoRepository.observarTodas(),
        erro
    ) { c, dividas, operacoes, mensagem ->
        montarPlanejamento(
            LocalDate.now(), c.contas, c.categorias, c.orcamentos, c.metas, c.aportes, dividas, operacoes
        ).copy(erro = mensagem)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanejamentoUiState())

    fun limparErro() {
        erro.value = null
    }

    // ---------- Orçamentos ----------

    /** Um orçamento por categoria: definir de novo altera o limite do que já existe. */
    fun definirOrcamento(categoriaId: Long, limite: Money) = executar {
        if (!limite.isPositivo) throw RegraFinanceiraException("O limite do orçamento deve ser maior que zero.")
        val existente = estado.value.orcamentos
            .firstOrNull { it.acompanhamento.orcamento.categoriaId == categoriaId }
            ?.acompanhamento?.orcamento
        orcamentoRepository.salvar(
            existente?.copy(limiteMensal = limite) ?: Orcamento(categoriaId = categoriaId, limiteMensal = limite)
        )
    }

    fun removerOrcamento(id: Long) = executar { orcamentoRepository.arquivar(id) }

    // ---------- Metas ----------

    fun novaMeta(nome: String, tipo: TipoMeta, valorAlvo: Money, prazo: LocalDate?) = executar {
        if (nome.isBlank()) throw RegraFinanceiraException("Dê um nome para a meta.")
        if (!valorAlvo.isPositivo) throw RegraFinanceiraException("O valor da meta deve ser maior que zero.")
        metaRepository.salvar(Meta(nome = nome.trim(), tipo = tipo, valorAlvo = valorAlvo, prazo = prazo))
    }

    /** [valor] positivo guarda; negativo retira. */
    fun movimentarMeta(progresso: ProgressoDaMeta, valor: Money) = executar {
        Metas.validarAporte(progresso.acumulado, valor)
        metaRepository.registrarAporte(AporteMeta(metaId = progresso.meta.id, data = LocalDate.now(), valor = valor))
    }

    fun arquivarMeta(id: Long) = executar { metaRepository.arquivar(id) }

    // ---------- Dívidas ----------

    fun novaDivida(
        credor: String,
        valor: Money,
        taxaMensalPercentual: BigDecimal,
        parcelas: Int,
        sistema: SistemaAmortizacao,
        primeiraParcela: LocalDate
    ) = executar {
        if (credor.isBlank()) throw RegraFinanceiraException("Informe o credor.")
        // Monta a tabela antes de salvar: se os dados forem inválidos, a regra recusa aqui.
        Amortizacao.tabela(valor, taxaMensalPercentual, parcelas, sistema, primeiraParcela)
        dividaRepository.salvar(
            Divida(
                credor = credor.trim(),
                valorOriginal = valor,
                taxaMensalPercentual = taxaMensalPercentual,
                parcelas = parcelas,
                sistema = sistema,
                primeiraParcela = primeiraParcela
            )
        )
    }

    /**
     * Marca a próxima parcela como paga. Com [contaId], também registra a despesa
     * no livro financeiro (primeiro a despesa, depois a baixa da parcela).
     */
    fun pagarParcela(item: DividaNaTela, contaId: Long?) = executar {
        val parcela = item.resumo.proximaParcela
            ?: throw RegraFinanceiraException("Esta dívida já está quitada.")
        if (contaId != null) {
            gestao.registrar(
                Operacao(
                    id = gestao.novoId(),
                    tipo = TipoOperacao.DESPESA,
                    data = LocalDate.now(),
                    valor = parcela.valor,
                    contaId = contaId,
                    descricao = "Parcela ${parcela.numero}/${item.resumo.totalDeParcelas} — ${item.divida.credor}"
                )
            )
        }
        dividaRepository.salvar(item.divida.copy(parcelasPagas = item.resumo.parcelasPagas + 1))
    }

    fun arquivarDivida(id: Long) = executar { dividaRepository.arquivar(id) }

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
            orcamentoRepository: OrcamentoRepository,
            metaRepository: MetaRepository,
            dividaRepository: DividaRepository,
            operacaoRepository: OperacaoRepository,
            gestao: GestaoFinanceira
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PlanejamentoViewModel(
                    contaRepository, categoriaRepository, orcamentoRepository, metaRepository,
                    dividaRepository, operacaoRepository, gestao
                )
            }
        }
    }
}
