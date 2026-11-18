package com.jfpfinance.core

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun somaESubtracaoSaoExatas() {
        // 0,10 + 0,20 em Double daria 0,30000000000000004
        assertEquals(Money(30), Money(10) + Money(20))
        assertEquals(Money(-5), Money(10) - Money(15))
    }

    @Test
    fun parcelasSomamOValorOriginal() {
        val parcelas = Money(10000).dividirEm(3)
        assertEquals(listOf(Money(3334), Money(3333), Money(3333)), parcelas)
        assertEquals(Money(10000), parcelas.somar())
    }

    @Test
    fun arredondaMetadeParaCima() {
        assertEquals(Money(101), Money.deDecimal(BigDecimal("1.005")))
        assertEquals(Money(100), Money.deDecimal(BigDecimal("1.004")))
        // 1% de R$ 0,50 = 0,005 -> R$ 0,01
        assertEquals(Money(1), Money(50).vezes(BigDecimal("0.01")))
    }

    @Test
    fun leTextoDigitadoPeloUsuario() {
        assertEquals(Money(123456), Money.deTexto("1.234,56"))
        assertEquals(Money(123456), Money.deTexto("1234,56"))
        assertEquals(Money(123456), Money.deTexto("1234.56"))
        assertEquals(Money(5000), Money.deTexto("R$ 50"))
        assertNull(Money.deTexto("abc"))
        assertNull(Money.deTexto(""))
    }

    @Test
    fun formataEmReais() {
        assertEquals("R$ 1.234,56", Money(123456).formatarBRL())
        assertEquals("R$ 0,05", Money(5).formatarBRL())
        assertEquals("-R$ 1.000.000,00", Money(-100000000).formatarBRL())
    }
}
