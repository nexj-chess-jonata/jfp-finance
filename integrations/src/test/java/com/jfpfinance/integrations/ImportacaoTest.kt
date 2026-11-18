package com.jfpfinance.integrations

import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.OrigemOperacao
import com.jfpfinance.core.model.TipoCategoria
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.engine.LivroFinanceiro
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportacaoTest {

    private fun reais(valor: String) = Money.deTexto(valor)!!

    private val ofxSgml = """
        OFXHEADER:100
        DATA:OFXSGML
        <OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>
        <STMTTRN>
        <TRNTYPE>DEBIT
        <DTPOSTED>20261105120000[-3:BRT]
        <TRNAMT>-150.75
        <FITID>202611050001
        <MEMO>SUPERMERCADO BOM PRECO
        </STMTTRN>
        <STMTTRN>
        <TRNTYPE>CREDIT
        <DTPOSTED>20261106
        <TRNAMT>3000,00
        <FITID>202611060002
        <NAME>SALARIO
        <MEMO>EMPRESA X &amp; CIA
        </STMTTRN>
        <STMTTRN>
        <DTPOSTED>20261340
        <TRNAMT>-10.00
        <FITID>ruim
        </STMTTRN>
        </BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
    """.trimIndent()

    // ---------- OFX ----------

    @Test
    fun leOfxAntigoComSinalDataEDescricao() {
        val r = LeitorOfx.ler(ofxSgml)
        assertEquals(2, r.lancamentos.size)
        val compra = r.lancamentos[0]
        assertEquals(LocalDate.of(2026, 11, 5), compra.data)
        assertEquals(reais("-150,75"), compra.valor)
        assertEquals("SUPERMERCADO BOM PRECO", compra.descricao)
        assertEquals("202611050001", compra.idExterno)
        val salario = r.lancamentos[1]
        assertEquals(reais("3000"), salario.valor)
        assertEquals("SALARIO - EMPRESA X & CIA", salario.descricao)
        assertEquals(1, r.avisos.size)
        assertTrue(r.avisos.single().contains("data inválida"))
    }

    @Test
    fun leOfxEmXmlNaMesmaLinha() {
        val xml = "<OFX><STMTRS><BANKTRANLIST><STMTTRN><DTPOSTED>20261101</DTPOSTED><TRNAMT>-20.00</TRNAMT>" +
            "<FITID>a1</FITID><MEMO>Padaria</MEMO></STMTTRN><STMTTRN><DTPOSTED>20261102</DTPOSTED>" +
            "<TRNAMT>-5.50</TRNAMT><FITID>a2</FITID><MEMO>Café</MEMO></STMTTRN></BANKTRANLIST></STMTRS></OFX>"
        val r = LeitorOfx.ler(xml)
        assertEquals(listOf("Padaria", "Café"), r.lancamentos.map { it.descricao })
        assertEquals(reais("-5,50"), r.lancamentos[1].valor)
        assertTrue(r.avisos.isEmpty())
    }

    @Test
    fun ofxVazioAvisa() {
        val r = LeitorOfx.ler("<OFX></OFX>")
        assertTrue(r.lancamentos.isEmpty())
        assertTrue(r.avisos.single().contains("Nenhum lançamento"))
    }

    // ---------- CSV ----------

    @Test
    fun leCsvBrasileiroComCabecalhoEAspas() {
        val csv = "Data;Histórico;Valor\n" +
            "05/11/2026;\"Mercado; filial 2\";-1.234,56\n" +
            "06/11/2026;Salário;R\$ 3.000,00\n" +
            "data ruim;x;10,00\n" +
            "07/11/2026;sem valor;\n"
        val r = LeitorCsv.ler(csv)
        assertEquals(2, r.lancamentos.size)
        assertEquals("Mercado; filial 2", r.lancamentos[0].descricao)
        assertEquals(reais("-1234,56"), r.lancamentos[0].valor)
        assertEquals(reais("3000"), r.lancamentos[1].valor)
        assertEquals(2, r.avisos.size)
        assertTrue(r.avisos[0].contains("Linha 4"))
        assertTrue(r.avisos[1].contains("Linha 5"))
    }

    @Test
    fun leCsvComVirgulaDataIsoEColunasDeCreditoEDebito() {
        val csv = "date,description,credit,debit\n" +
            "2026-11-05,Coffee,,4.50\n" +
            "2026-11-06,Refund,10.00,\n"
        val r = LeitorCsv.ler(csv)
        assertEquals(reais("-4,50"), r.lancamentos[0].valor)
        assertEquals(reais("10"), r.lancamentos[1].valor)
        assertEquals(LocalDate.of(2026, 11, 6), r.lancamentos[1].data)
    }

    @Test
    fun csvSemCabecalhoAssumeColunasEAvisa() {
        val r = LeitorCsv.ler("05/11/26;Padaria;-12,00\n")
        assertEquals(LocalDate.of(2026, 11, 5), r.lancamentos.single().data)
        assertEquals(reais("-12"), r.lancamentos.single().valor)
        assertTrue(r.avisos.single().contains("Cabeçalho não reconhecido"))
    }

    @Test
    fun valoresEmVariosFormatos() {
        assertEquals(reais("-1234,56"), lerValor("(1.234,56)"))
        assertEquals(reais("-1234,56"), lerValor("1.234,56-"))
        assertEquals(reais("1234,56"), lerValor("1,234.56"))
        assertEquals(reais("1234,56"), lerValor("+1234.56"))
        // Sem centavos, o ponto é milhar: "1.500" são mil e quinhentos reais, não um real e cinquenta.
        assertEquals(reais("1500"), lerValor("1.500"))
        assertEquals(reais("-12345678"), lerValor("-12.345.678"))
        assertEquals(reais("1,50"), lerValor("1.50"))
        assertNull(lerValor("abc"))
        assertNull(lerValor("12,34,56x"))
        assertNull(lerData("31/02/2026"))
    }

    @Test
    fun decodificaUtf8EWindows1252() {
        assertEquals("Salário", Arquivos.decodificar("Salário".toByteArray(Charsets.UTF_8)))
        assertEquals("Salário", Arquivos.decodificar("Salário".toByteArray(charset("windows-1252"))))
        assertEquals("Data", Arquivos.decodificar(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "Data".toByteArray()))
        assertTrue(Arquivos.ler("extrato.xlsx", ByteArray(4)).avisos.single().contains("Excel"))
        assertEquals(2, Arquivos.ler("extrato.txt", ofxSgml.toByteArray()).lancamentos.size)
    }

    // ---------- Plano ----------

    private val alimentacao = Categoria(id = 100, nome = "Alimentação", tipo = TipoCategoria.DESPESA)
    private val lidos = LeitorOfx.ler(ofxSgml).lancamentos

    private fun plano(existentes: List<Operacao> = emptyList()) =
        PlanoDeImportacao.montar(lidos, contaId = 1, existentes = existentes, categorias = listOf(alimentacao), nomeDoArquivo = "extrato.ofx", agora = 7)

    @Test
    fun planoPropoeDespesaEReceitaMarcadasComoImportadas() {
        val itens = plano()
        assertEquals(listOf(TipoOperacao.DESPESA, TipoOperacao.RECEITA), itens.map { it.operacao.tipo })
        assertTrue(itens.all { it.operacao.valor.isPositivo })
        assertTrue(itens.all { it.operacao.origem == OrigemOperacao.IMPORTACAO && it.operacao.origemDetalhe == "extrato.ofx" })
        assertTrue(itens.all { it.situacao == SituacaoDoItem.NOVO && it.selecionado })
        // As propostas passam pelas regras do motor.
        assertEquals(2, LivroFinanceiro.de(itens.map { it.operacao }).operacoes.size)
    }

    @Test
    fun reimportarOMesmoArquivoNaoDuplica() {
        val primeira = plano().map { it.operacao }
        val segunda = plano(existentes = primeira)
        assertTrue(segunda.all { it.situacao == SituacaoDoItem.JA_IMPORTADO && !it.selecionado })
        assertEquals(primeira.map { it.id }, segunda.map { it.operacao.id })
        // Mesmo que o usuário marque tudo de novo, o motor não grava em dobro.
        val livro = LivroFinanceiro.de(primeira)
        assertEquals(2, segunda.fold(livro) { l, item -> l.registrar(item.operacao.copy(criadaEm = 99)) }.operacoes.size)
    }

    @Test
    fun lancamentoManualIgualViraPossivelDuplicado() {
        val manual = Operacao(
            id = "manual-1", tipo = TipoOperacao.DESPESA, data = LocalDate.of(2026, 11, 5),
            valor = reais("150,75"), contaId = 1, descricao = "mercado", criadaEm = 1
        )
        val itens = plano(existentes = listOf(manual))
        assertEquals(SituacaoDoItem.POSSIVEL_DUPLICADO, itens[0].situacao)
        assertFalse(itens[0].selecionado)
        assertEquals(SituacaoDoItem.NOVO, itens[1].situacao)
        // Em outra conta não é duplicado.
        assertEquals(SituacaoDoItem.NOVO, plano(existentes = listOf(manual.copy(contaId = 2)))[0].situacao)
    }

    @Test
    fun linhasIdenticasNoMesmoArquivoSaoLancamentosDiferentes() {
        val cafe = LancamentoImportado(LocalDate.of(2026, 11, 5), reais("-5"), "Café")
        val itens = PlanoDeImportacao.montar(listOf(cafe, cafe), 1, emptyList(), emptyList(), "a.csv", 1)
        assertEquals(2, itens.map { it.operacao.id }.distinct().size)
        // E continuam estáveis numa segunda importação.
        val de_novo = PlanoDeImportacao.montar(listOf(cafe, cafe), 1, itens.map { it.operacao }, emptyList(), "a.csv", 2)
        assertTrue(de_novo.all { it.situacao == SituacaoDoItem.JA_IMPORTADO })
    }

    @Test
    fun sugereCategoriaPelaDescricaoJaUsada() {
        val anterior = Operacao(
            id = "x", tipo = TipoOperacao.DESPESA, data = LocalDate.of(2026, 10, 5), valor = reais("90"),
            contaId = 1, categoriaId = 100, descricao = "supermercado  bom preco", criadaEm = 1
        )
        val itens = plano(existentes = listOf(anterior))
        assertEquals(100L, itens[0].operacao.categoriaId)
        assertNull(itens[1].operacao.categoriaId)
    }
}
