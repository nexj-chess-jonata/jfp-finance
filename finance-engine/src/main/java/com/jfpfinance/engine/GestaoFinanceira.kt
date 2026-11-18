package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.OrigemOperacao
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.core.repository.OperacaoRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Única porta de entrada para alterar o livro financeiro.
 *
 * Toda alteração: carrega o livro, aplica as regras do motor e só então grava.
 * Se qualquer regra falhar, nada é gravado. As alterações são feitas uma de cada vez.
 *
 * Observação de desempenho: o livro inteiro é recarregado e revalidado a cada alteração.
 * É simples e seguro; se o histórico crescer muito, otimizar aqui sem mudar as telas.
 */
class GestaoFinanceira(
    private val repositorio: OperacaoRepository,
    private val gerarId: () -> String = { UUID.randomUUID().toString() },
    private val agora: () -> Long = { System.currentTimeMillis() }
) {
    private val trava = Mutex()

    fun novoId(): String = gerarId()

    suspend fun registrar(operacoes: List<Operacao>) = alterar { livro ->
        operacoes.fold(livro) { atual, operacao -> atual.registrar(operacao) }
    }

    suspend fun registrar(operacao: Operacao) = registrar(listOf(operacao))

    suspend fun registrarParcelado(modelo: Operacao, parcelas: Int) =
        registrar(Geradores.parcelar(modelo, parcelas, gerarId))

    suspend fun registrarRepetindo(modelo: Operacao, vezes: Int) =
        registrar(Geradores.repetirMensalmente(modelo, vezes, gerarId))

    suspend fun estornar(id: String, data: LocalDate) = alterar { livro ->
        livro.reverter(id, gerarId(), data, agora())
    }

    suspend fun confirmar(id: String, dataEfetiva: LocalDate) = alterar { it.confirmar(id, dataEfetiva) }

    suspend fun cancelarPrevista(id: String) = alterar { it.cancelarPrevista(id) }

    suspend fun pagarFatura(cartaoId: Long, contaId: Long, valor: Money, data: LocalDate, moeda: String = "BRL") =
        registrar(
            Operacao(
                id = gerarId(),
                tipo = TipoOperacao.PAGAMENTO_FATURA,
                data = data,
                valor = valor,
                moeda = moeda,
                contaId = contaId,
                cartaoId = cartaoId,
                descricao = "Pagamento de fatura",
                origem = OrigemOperacao.MANUAL,
                criadaEm = agora()
            )
        )

    private suspend fun alterar(acao: (LivroFinanceiro) -> LivroFinanceiro) {
        trava.withLock {
            val antes = repositorio.listarTodas()
            val porId = antes.associateBy { it.id }
            val depois = acao(LivroFinanceiro.de(antes)).operacoes
            val alteradas = depois.filter { porId[it.id] != it }
            if (alteradas.isNotEmpty()) repositorio.gravar(alteradas)
        }
    }
}
