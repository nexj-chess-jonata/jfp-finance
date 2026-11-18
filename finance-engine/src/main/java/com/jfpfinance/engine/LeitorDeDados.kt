package com.jfpfinance.engine

import com.jfpfinance.core.model.AporteMeta
import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Investimento
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.MovimentoInvestimento
import com.jfpfinance.core.model.Orcamento
import com.jfpfinance.core.repository.BemRepository
import com.jfpfinance.core.repository.CartaoRepository
import com.jfpfinance.core.repository.CategoriaRepository
import com.jfpfinance.core.repository.ContaRepository
import com.jfpfinance.core.repository.DividaRepository
import com.jfpfinance.core.repository.InvestimentoRepository
import com.jfpfinance.core.repository.MetaRepository
import com.jfpfinance.core.repository.OperacaoRepository
import com.jfpfinance.core.repository.OrcamentoRepository
import com.jfpfinance.core.repository.PessoaRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Junta todos os cadastros e o livro em um único retrato ([DadosFinanceiros]) para as
 * análises, o assistente e os alertas. Só leitura: não tem nenhum método que grave.
 */
class LeitorDeDados(
    private val contas: ContaRepository,
    private val categorias: CategoriaRepository,
    private val cartoes: CartaoRepository,
    private val orcamentos: OrcamentoRepository,
    private val metas: MetaRepository,
    private val dividas: DividaRepository,
    private val bens: BemRepository,
    private val investimentos: InvestimentoRepository,
    private val operacoes: OperacaoRepository,
    private val pessoas: PessoaRepository,
    private val hoje: () -> LocalDate = { LocalDate.now() }
) {
    private data class Planejamento(
        val contas: List<Conta>,
        val categorias: List<Categoria>,
        val cartoes: List<Cartao>,
        val orcamentos: List<Orcamento>,
        val metas: List<Meta>
    )

    private data class Patrimonio(
        val aportes: List<AporteMeta>,
        val dividas: List<Divida>,
        val bens: List<Bem>,
        val investimentos: List<Investimento>,
        val movimentos: List<MovimentoInvestimento>
    )

    fun observar(): Flow<DadosFinanceiros> {
        val planejamento = combine(
            contas.observarContasAtivas(),
            categorias.observarAtivas(),
            cartoes.observarAtivos(),
            orcamentos.observarAtivos(),
            metas.observarAtivas()
        ) { a, b, c, d, e -> Planejamento(a, b, c, d, e) }

        val patrimonio = combine(
            metas.observarAportes(),
            dividas.observarAtivas(),
            bens.observarAtivos(),
            investimentos.observarAtivos(),
            investimentos.observarMovimentos()
        ) { a, b, c, d, e -> Patrimonio(a, b, c, d, e) }

        return combine(planejamento, patrimonio, operacoes.observarTodas(), pessoas.observarAtivas()) { p, q, livro, gente ->
            DadosFinanceiros(
                hoje = hoje(),
                contas = p.contas,
                categorias = p.categorias,
                cartoes = p.cartoes,
                orcamentos = p.orcamentos,
                metas = p.metas,
                aportes = q.aportes,
                dividas = q.dividas,
                bens = q.bens,
                investimentos = q.investimentos,
                movimentos = q.movimentos,
                operacoes = livro,
                pessoas = gente
            )
        }
    }
}
