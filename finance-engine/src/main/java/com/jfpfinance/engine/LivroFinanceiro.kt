package com.jfpfinance.engine

import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.OrigemOperacao
import com.jfpfinance.core.model.SituacaoOperacao
import java.time.LocalDate

/**
 * Livro financeiro central (seção 28.1).
 *
 * Imutável: cada alteração devolve um novo livro, então uma alteração que falha
 * não deixa nada pela metade.
 *
 * Operações confirmadas nunca são apagadas nem editadas. Para corrigir, usa-se
 * [reverter] (cria o estorno) ou [corrigir] (estorno + novo registro).
 * Operações previstas ainda não são histórico e podem ser confirmadas ou canceladas.
 */
class LivroFinanceiro private constructor(private val porId: Map<String, Operacao>) {

    val operacoes: List<Operacao> get() = porId.values.toList()

    fun buscar(id: String): Operacao? = porId[id]

    fun foiRevertida(id: String): Boolean = porId.values.any { it.reverteId == id }

    /**
     * Registra uma operação. Registrar de novo a mesma operação (mesmo id, mesmos dados)
     * não faz nada — isso protege contra toque duplo e importação repetida.
     */
    fun registrar(operacao: Operacao): LivroFinanceiro {
        operacao.validar()
        val existente = porId[operacao.id]
        if (existente != null) {
            if (existente.copy(criadaEm = 0) == operacao.copy(criadaEm = 0)) return this
            throw RegraFinanceiraException("Já existe outra operação com o identificador ${operacao.id}.")
        }
        if (operacao.reverteId != null) validarReversao(operacao)
        return LivroFinanceiro(porId + (operacao.id to operacao))
    }

    /** Cria o estorno de uma operação confirmada. O original permanece no histórico. */
    fun reverter(
        id: String,
        idDaReversao: String,
        data: LocalDate,
        agora: Long = System.currentTimeMillis()
    ): LivroFinanceiro {
        val original = porId[id] ?: throw RegraFinanceiraException("Operação $id não encontrada.")
        val estorno = original.copy(
            id = idDaReversao,
            data = data,
            reverteId = original.id,
            origem = OrigemOperacao.SISTEMA,
            descricao = "Estorno: ${original.descricao}".trim(),
            criadaEm = agora
        )
        return registrar(estorno)
    }

    /** Corrige uma operação confirmada: estorna a antiga e registra a nova, tudo ou nada. */
    fun corrigir(
        id: String,
        idDaReversao: String,
        corrigida: Operacao,
        dataDaCorrecao: LocalDate,
        agora: Long = System.currentTimeMillis()
    ): LivroFinanceiro {
        if (corrigida.id == id || corrigida.id == idDaReversao) {
            throw RegraFinanceiraException("A operação corrigida precisa de um identificador novo.")
        }
        return reverter(id, idDaReversao, dataDaCorrecao, agora).registrar(corrigida)
    }

    /** Uma operação prevista aconteceu: passa a confirmada na data informada. */
    fun confirmar(id: String, dataEfetiva: LocalDate): LivroFinanceiro {
        val prevista = exigirPrevista(id)
        return LivroFinanceiro(
            porId + (id to prevista.copy(situacao = SituacaoOperacao.CONFIRMADA, data = dataEfetiva))
        )
    }

    /** Uma operação prevista não vai mais acontecer. Fica registrada como cancelada. */
    fun cancelarPrevista(id: String): LivroFinanceiro {
        val prevista = exigirPrevista(id)
        return LivroFinanceiro(porId + (id to prevista.copy(situacao = SituacaoOperacao.CANCELADA)))
    }

    private fun exigirPrevista(id: String): Operacao {
        val operacao = porId[id] ?: throw RegraFinanceiraException("Operação $id não encontrada.")
        if (operacao.situacao != SituacaoOperacao.PREVISTA) {
            throw RegraFinanceiraException("Só operações previstas podem ser confirmadas ou canceladas.")
        }
        return operacao
    }

    private fun validarReversao(estorno: Operacao) {
        val original = porId[estorno.reverteId]
            ?: throw RegraFinanceiraException("A operação a estornar não foi encontrada.")
        if (original.situacao != SituacaoOperacao.CONFIRMADA) {
            throw RegraFinanceiraException("Só operações confirmadas podem ser estornadas.")
        }
        if (original.ehReversao) throw RegraFinanceiraException("Um estorno não pode ser estornado.")
        if (foiRevertida(original.id)) throw RegraFinanceiraException("Esta operação já foi estornada.")
        if (estorno.data.isBefore(original.data)) {
            throw RegraFinanceiraException("O estorno não pode ter data anterior à operação original.")
        }
        val mesmosDados = estorno.tipo == original.tipo &&
            estorno.valor == original.valor &&
            estorno.moeda == original.moeda &&
            estorno.contaId == original.contaId &&
            estorno.contaDestinoId == original.contaDestinoId &&
            estorno.cartaoId == original.cartaoId &&
            estorno.categoriaId == original.categoriaId
        if (!mesmosDados) throw RegraFinanceiraException("O estorno deve espelhar a operação original.")
        if (estorno.situacao != SituacaoOperacao.CONFIRMADA) {
            throw RegraFinanceiraException("O estorno deve ser uma operação confirmada.")
        }
    }

    companion object {
        fun vazio(): LivroFinanceiro = LivroFinanceiro(LinkedHashMap())

        /** Reconstrói o livro a partir de operações já gravadas, validando todas. */
        fun de(operacoes: Iterable<Operacao>): LivroFinanceiro =
            operacoes.fold(vazio()) { livro, operacao -> livro.registrar(operacao) }
    }
}
