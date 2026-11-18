package com.jfpfinance.integrations

import com.jfpfinance.core.Money
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.DateTimeException
import java.time.LocalDate

/** Uma linha lida de um extrato. [valor] positivo = entrada; negativo = saída. */
data class LancamentoImportado(
    val data: LocalDate,
    val valor: Money,
    val descricao: String,
    /** Identificador que o banco deu ao lançamento (FITID do OFX), quando existe. */
    val idExterno: String? = null
)

data class ResultadoDaLeitura(
    val lancamentos: List<LancamentoImportado>,
    /** Linhas que não puderam ser lidas, com o motivo. Nada é descartado em silêncio. */
    val avisos: List<String>
)

object Arquivos {

    /** Extratos de bancos brasileiros vêm em UTF-8 ou em Windows-1252. Tenta UTF-8 primeiro. */
    fun decodificar(bytes: ByteArray): String {
        val semBom = if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            bytes.copyOfRange(3, bytes.size)
        } else {
            bytes
        }
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(semBom)).toString()
        } catch (e: CharacterCodingException) {
            String(semBom, charset("windows-1252"))
        }
    }

    /** Escolhe o leitor pelo nome do arquivo e, na dúvida, pelo conteúdo. */
    fun ler(nomeDoArquivo: String, bytes: ByteArray): ResultadoDaLeitura {
        val texto = decodificar(bytes)
        val nome = nomeDoArquivo.lowercase()
        return when {
            nome.endsWith(".ofx") || texto.contains("<OFX", ignoreCase = true) -> LeitorOfx.ler(texto)
            nome.endsWith(".xlsx") || nome.endsWith(".xls") -> ResultadoDaLeitura(
                emptyList(),
                listOf("Planilhas do Excel ainda não são lidas. No Excel, use \"Salvar como\" e escolha CSV.")
            )
            else -> LeitorCsv.ler(texto)
        }
    }
}

internal fun lerValor(texto: String): Money? {
    var limpo = texto.replace("R$", "").replace(" ", "").replace(" ", "").trim()
    if (limpo.isEmpty()) return null
    var negativo = false
    if (limpo.startsWith("(") && limpo.endsWith(")")) {
        negativo = true
        limpo = limpo.substring(1, limpo.length - 1)
    }
    if (limpo.endsWith("-")) {
        negativo = true
        limpo = limpo.dropLast(1)
    }
    if (limpo.startsWith("-")) {
        negativo = !negativo
        limpo = limpo.drop(1)
    } else if (limpo.startsWith("+")) {
        limpo = limpo.drop(1)
    }
    val normalizado = when {
        // 1.234,56 ou 1234,56: vírgula é o decimal
        limpo.contains(',') && limpo.lastIndexOf(',') > limpo.lastIndexOf('.') -> limpo.replace(".", "").replace(',', '.')
        // 1,234.56: ponto é o decimal
        limpo.contains(',') -> limpo.replace(",", "")
        // 1.500 ou 12.345.678, sem vírgula: no Brasil o ponto é separador de milhar, não decimal.
        limpo.matches(Regex("\\d{1,3}(\\.\\d{3})+")) -> limpo.replace(".", "")
        else -> limpo
    }
    if (!normalizado.matches(Regex("\\d+(\\.\\d+)?"))) return null
    return try {
        val valor = Money.deDecimal(BigDecimal(normalizado))
        if (negativo) -valor else valor
    } catch (e: ArithmeticException) {
        null
    }
}

internal fun lerData(texto: String): LocalDate? {
    val limpo = texto.trim()
    fun montar(ano: Int, mes: Int, dia: Int): LocalDate? = try {
        LocalDate.of(ano, mes, dia)
    } catch (e: DateTimeException) {
        null
    }
    Regex("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})").find(limpo)?.let { m ->
        return montar(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt())
    }
    Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(limpo)?.let { m ->
        return montar(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
    }
    Regex("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2})$").find(limpo)?.let { m ->
        return montar(2000 + m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt())
    }
    return null
}

/** Extrato OFX (seção 24): aceita o formato antigo (SGML, sem fechar marcas) e o XML. */
object LeitorOfx {

    private val BLOCO = Regex("<STMTTRN>(.*?)(?=<STMTTRN>|</BANKTRANLIST>|</CCSTMTRS>|</STMTRS>|\\z)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    private fun campo(bloco: String, nome: String): String? =
        Regex("<$nome>([^<\\r\\n]*)", RegexOption.IGNORE_CASE).find(bloco)?.groupValues?.get(1)?.trim()
            ?.replace("&amp;", "&")?.replace("&lt;", "<")?.replace("&gt;", ">")
            ?.takeIf { it.isNotEmpty() }

    fun ler(texto: String): ResultadoDaLeitura {
        val avisos = mutableListOf<String>()
        val blocos = BLOCO.findAll(texto).map { it.groupValues[1] }.toList()
        if (blocos.isEmpty()) avisos.add("Nenhum lançamento encontrado no arquivo OFX.")

        val lancamentos = blocos.mapIndexedNotNull { indice, bloco ->
            val numero = indice + 1
            val dataTexto = campo(bloco, "DTPOSTED")
            val data = dataTexto?.takeIf { it.length >= 8 && it.take(8).all(Char::isDigit) }?.let {
                try {
                    LocalDate.of(it.substring(0, 4).toInt(), it.substring(4, 6).toInt(), it.substring(6, 8).toInt())
                } catch (e: DateTimeException) {
                    null
                }
            }
            // No OFX o decimal é ponto; alguns bancos brasileiros usam vírgula.
            val valor = campo(bloco, "TRNAMT")?.let { lerValor(it) }
            when {
                data == null -> {
                    avisos.add("Lançamento $numero ignorado: data inválida (${dataTexto ?: "ausente"}).")
                    null
                }
                valor == null -> {
                    avisos.add("Lançamento $numero ignorado: valor inválido (${campo(bloco, "TRNAMT") ?: "ausente"}).")
                    null
                }
                valor.isZero -> {
                    avisos.add("Lançamento $numero ignorado: valor zero.")
                    null
                }
                else -> LancamentoImportado(
                    data = data,
                    valor = valor,
                    descricao = listOfNotNull(campo(bloco, "NAME"), campo(bloco, "MEMO")).distinct().joinToString(" - "),
                    idExterno = campo(bloco, "FITID")
                )
            }
        }
        return ResultadoDaLeitura(lancamentos, avisos)
    }
}

/**
 * Extrato CSV (seção 24). Descobre o separador e as colunas pelo cabeçalho
 * (data, descrição/histórico, valor, ou crédito e débito separados).
 * Sem cabeçalho reconhecível, assume: data, descrição, valor.
 */
object LeitorCsv {

    private val NOMES_DATA = listOf("data", "date", "dt")
    private val NOMES_DESCRICAO = listOf("descricao", "historico", "lancamento", "memo", "description", "detalhe", "titulo", "estabelecimento")
    private val NOMES_VALOR = listOf("valor", "amount", "value", "quantia", "montante")
    private val NOMES_CREDITO = listOf("credito", "entrada", "credit")
    private val NOMES_DEBITO = listOf("debito", "saida", "debit")

    fun ler(texto: String): ResultadoDaLeitura {
        val linhas = texto.lines().map { it.trimEnd('\r') }.withIndex().filter { it.value.isNotBlank() }
        if (linhas.isEmpty()) return ResultadoDaLeitura(emptyList(), listOf("O arquivo está vazio."))

        val separador = listOf(';', '\t', ',').maxByOrNull { candidato -> separar(linhas.first().value, candidato).size } ?: ';'
        val primeira = separar(linhas.first().value, separador).map(::normalizar)

        fun coluna(nomes: List<String>) = primeira.indexOfFirst { celula -> nomes.any { celula.startsWith(it) } }
        val temCabecalho = coluna(NOMES_DATA) >= 0 &&
            (coluna(NOMES_VALOR) >= 0 || coluna(NOMES_CREDITO) >= 0 || coluna(NOMES_DEBITO) >= 0)

        val colData = if (temCabecalho) coluna(NOMES_DATA) else 0
        val colDescricao = if (temCabecalho) coluna(NOMES_DESCRICAO) else 1
        val colValor = if (temCabecalho) coluna(NOMES_VALOR) else 2
        val colCredito = if (temCabecalho) coluna(NOMES_CREDITO) else -1
        val colDebito = if (temCabecalho) coluna(NOMES_DEBITO) else -1

        val avisos = mutableListOf<String>()
        if (!temCabecalho) avisos.add("Cabeçalho não reconhecido: as colunas foram lidas como data, descrição e valor. Confira a prévia.")

        val lancamentos = linhas.drop(if (temCabecalho) 1 else 0).mapNotNull { (indice, linha) ->
            val numero = indice + 1
            val celulas = separar(linha, separador)
            fun celula(coluna: Int) = celulas.getOrNull(coluna)?.trim().orEmpty()

            val data = lerData(celula(colData))
            val valor = when {
                colValor >= 0 && celula(colValor).isNotEmpty() -> lerValor(celula(colValor))
                else -> {
                    val credito = if (colCredito >= 0) lerValor(celula(colCredito)) else null
                    val debito = if (colDebito >= 0) lerValor(celula(colDebito)) else null
                    when {
                        credito != null && !credito.isZero -> if (credito.isNegativo) -credito else credito
                        debito != null && !debito.isZero -> if (debito.isPositivo) -debito else debito
                        else -> null
                    }
                }
            }
            when {
                data == null -> {
                    avisos.add("Linha $numero ignorada: data não reconhecida (\"${celula(colData)}\").")
                    null
                }
                valor == null || valor.isZero -> {
                    avisos.add("Linha $numero ignorada: valor ausente, zero ou não reconhecido.")
                    null
                }
                else -> LancamentoImportado(data, valor, if (colDescricao >= 0) celula(colDescricao) else "")
            }
        }
        return ResultadoDaLeitura(lancamentos, avisos)
    }

    /** Separa uma linha respeitando aspas ("a;b" é uma célula só; "" dentro de aspas é uma aspa). */
    internal fun separar(linha: String, separador: Char): List<String> {
        val celulas = mutableListOf<String>()
        val atual = StringBuilder()
        var entreAspas = false
        var i = 0
        while (i < linha.length) {
            val c = linha[i]
            when {
                c == '"' && entreAspas && i + 1 < linha.length && linha[i + 1] == '"' -> {
                    atual.append('"')
                    i++
                }
                c == '"' -> entreAspas = !entreAspas
                c == separador && !entreAspas -> {
                    celulas.add(atual.toString())
                    atual.clear()
                }
                else -> atual.append(c)
            }
            i++
        }
        celulas.add(atual.toString())
        return celulas
    }

    private fun normalizar(texto: String): String =
        java.text.Normalizer.normalize(texto.trim().lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}
