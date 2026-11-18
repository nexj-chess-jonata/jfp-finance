package com.jfpfinance.engine

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.core.somar
import java.time.LocalDate
import java.time.YearMonth

/**
 * Fatura de um cartão. Entram as compras com data de [inicio] (inclusive)
 * até a véspera de [fechamento].
 */
data class Fatura(
    val cartaoId: Long,
    val inicio: LocalDate,
    val fechamento: LocalDate,
    val vencimento: LocalDate,
    val total: Money,
    val pago: Money
) {
    val restante: Money get() = total - pago
    val quitada: Boolean get() = !restante.isPositivo
    fun fechadaEm(data: LocalDate): Boolean = !data.isBefore(fechamento)
}

object Faturas {

    private fun diaNoMes(mes: YearMonth, dia: Int): LocalDate = mes.atDay(minOf(dia, mes.lengthOfMonth()))

    /**
     * Data de fechamento da fatura em que uma compra entra.
     * Regra: compra feita até a véspera do fechamento entra na fatura que fecha naquele mês;
     * compra feita no dia do fechamento ou depois vai para a fatura seguinte.
     */
    fun fechamentoDaCompra(cartao: Cartao, dataDaCompra: LocalDate): LocalDate {
        val mes = YearMonth.from(dataDaCompra)
        val fechamento = diaNoMes(mes, cartao.diaFechamento)
        return if (dataDaCompra.isBefore(fechamento)) fechamento else diaNoMes(mes.plusMonths(1), cartao.diaFechamento)
    }

    /** O vencimento é o primeiro dia de vencimento depois do fechamento. */
    fun vencimentoDaFatura(cartao: Cartao, fechamento: LocalDate): LocalDate {
        val mes = YearMonth.from(fechamento)
        val noMesmoMes = diaNoMes(mes, cartao.diaVencimento)
        return if (noMesmoMes.isAfter(fechamento)) noMesmoMes else diaNoMes(mes.plusMonths(1), cartao.diaVencimento)
    }

    /**
     * Faturas do cartão, da mais antiga para a mais nova, incluindo as futuras que já têm parcelas.
     * Os pagamentos são abatidos da fatura mais antiga em aberto para a mais nova.
     */
    fun listar(cartao: Cartao, operacoes: Iterable<Operacao>): List<Fatura> {
        val doCartao = operacoes.filter {
            it.cartaoId == cartao.id && it.situacao == SituacaoOperacao.CONFIRMADA
        }
        val totaisPorFechamento = doCartao
            .filter { it.tipo == TipoOperacao.COMPRA_CARTAO }
            .groupBy { fechamentoDaCompra(cartao, it.data) }
            .mapValues { (_, compras) -> compras.map { it.efeitoNaDividaDoCartao(cartao.id) }.somar() }
            .toSortedMap()

        var saldoDePagamentos = -doCartao
            .filter { it.tipo == TipoOperacao.PAGAMENTO_FATURA }
            .map { it.efeitoNaDividaDoCartao(cartao.id) }
            .somar()

        return totaisPorFechamento.map { (fechamento, total) ->
            val pago = when {
                !total.isPositivo -> {
                    // Fatura com crédito (estornos maiores que compras): o crédito abate as próximas.
                    saldoDePagamentos -= total
                    Money.ZERO
                }
                saldoDePagamentos >= total -> total
                saldoDePagamentos.isPositivo -> saldoDePagamentos
                else -> Money.ZERO
            }
            if (total.isPositivo) saldoDePagamentos -= pago
            Fatura(
                cartaoId = cartao.id,
                inicio = diaNoMes(YearMonth.from(fechamento).minusMonths(1), cartao.diaFechamento),
                fechamento = fechamento,
                vencimento = vencimentoDaFatura(cartao, fechamento),
                total = total,
                pago = pago
            )
        }
    }

    /** Limite em uso: todas as compras (inclusive parcelas futuras) menos tudo o que já foi pago. */
    fun limiteUtilizado(cartao: Cartao, operacoes: Iterable<Operacao>): Money =
        CalculadoraDeSaldos.dividaDoCartao(cartao.id, operacoes, LocalDate.MAX)

    fun limiteDisponivel(cartao: Cartao, operacoes: Iterable<Operacao>): Money =
        cartao.limite - limiteUtilizado(cartao, operacoes)
}
