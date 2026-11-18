package com.jfpfinance.ui.planejamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfpfinance.core.Money
import com.jfpfinance.core.formatarBR
import com.jfpfinance.core.lerDataBR
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.SistemaAmortizacao
import com.jfpfinance.core.model.TipoMeta
import com.jfpfinance.ui.comum.CampoNumero
import com.jfpfinance.ui.comum.CampoTexto
import com.jfpfinance.ui.comum.CampoValor
import com.jfpfinance.ui.comum.LinhaValor
import com.jfpfinance.ui.comum.Seletor
import com.jfpfinance.ui.comum.TextoDeApoio
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun DialogoOrcamento(
    categorias: List<Categoria>,
    aoCancelar: () -> Unit,
    aoSalvar: (categoriaId: Long, limite: Money) -> Unit
) {
    var categoriaId by remember { mutableStateOf(categorias.firstOrNull()?.id) }
    var limiteTexto by remember { mutableStateOf("") }
    val limite = Money.deTexto(limiteTexto)?.takeIf { it.isPositivo }
    val categoriaEscolhida = categoriaId

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Orçamento da categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Seletor(
                    rotulo = "Categoria",
                    opcoes = categorias,
                    selecionado = categorias.firstOrNull { it.id == categoriaId },
                    texto = { it.nome },
                    aoSelecionar = { categoriaId = it.id }
                )
                CampoValor(limiteTexto, { limiteTexto = it }, "Limite por mês (R$)", invalido = limiteTexto.isNotBlank() && limite == null)
                TextoDeApoio("Se a categoria já tiver orçamento, o limite é substituído.")
            }
        },
        confirmButton = {
            TextButton(
                enabled = limite != null && categoriaEscolhida != null,
                onClick = { if (limite != null && categoriaEscolhida != null) aoSalvar(categoriaEscolhida, limite) }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoNovaMeta(
    aoCancelar: () -> Unit,
    aoSalvar: (nome: String, tipo: TipoMeta, valorAlvo: Money, prazo: LocalDate?) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoMeta.RESERVA_EMERGENCIA) }
    var alvoTexto by remember { mutableStateOf("") }
    var prazoTexto by remember { mutableStateOf("") }

    val alvo = Money.deTexto(alvoTexto)?.takeIf { it.isPositivo }
    val prazo = if (prazoTexto.isBlank()) null else lerDataBR(prazoTexto)
    val prazoInvalido = prazoTexto.isNotBlank() && prazo == null

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Nova meta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTexto(nome, { nome = it }, "Nome da meta")
                Seletor(
                    rotulo = "Tipo",
                    opcoes = TipoMeta.entries.toList(),
                    selecionado = tipo,
                    texto = { it.rotulo },
                    aoSelecionar = { tipo = it }
                )
                CampoValor(alvoTexto, { alvoTexto = it }, "Valor do objetivo (R$)", invalido = alvoTexto.isNotBlank() && alvo == null)
                CampoTexto(prazoTexto, { prazoTexto = it }, "Prazo (dd/mm/aaaa) — opcional", invalido = prazoInvalido)
            }
        },
        confirmButton = {
            TextButton(
                enabled = nome.isNotBlank() && alvo != null && !prazoInvalido,
                onClick = { if (alvo != null) aoSalvar(nome, tipo, alvo, prazo) }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

/** Pede um valor positivo em reais. */
@Composable
fun DialogoValor(
    titulo: String,
    explicacao: String,
    aoCancelar: () -> Unit,
    aoSalvar: (Money) -> Unit
) {
    var texto by remember { mutableStateOf("") }
    val valor = Money.deTexto(texto)?.takeIf { it.isPositivo }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoValor(texto, { texto = it }, "Valor (R$)", invalido = texto.isNotBlank() && valor == null)
                TextoDeApoio(explicacao)
            }
        },
        confirmButton = {
            TextButton(enabled = valor != null, onClick = { if (valor != null) aoSalvar(valor) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoNovaDivida(
    hoje: LocalDate,
    aoCancelar: () -> Unit,
    aoSalvar: (
        credor: String,
        valor: Money,
        taxaMensalPercentual: BigDecimal,
        parcelas: Int,
        sistema: SistemaAmortizacao,
        primeiraParcela: LocalDate
    ) -> Unit
) {
    var credor by remember { mutableStateOf("") }
    var valorTexto by remember { mutableStateOf("") }
    var taxaTexto by remember { mutableStateOf("") }
    var parcelasTexto by remember { mutableStateOf("") }
    var sistema by remember { mutableStateOf(SistemaAmortizacao.PRICE) }
    var primeiraTexto by remember { mutableStateOf(hoje.plusMonths(1).formatarBR()) }

    val valor = Money.deTexto(valorTexto)?.takeIf { it.isPositivo }
    val taxa = lerTaxa(taxaTexto)
    val parcelas = parcelasTexto.toIntOrNull()?.takeIf { it in 1..600 }
    val primeira = lerDataBR(primeiraTexto)
    val previa = simularDivida(valor, taxa, parcelas, sistema, primeira)

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Dívida ou financiamento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CampoTexto(credor, { credor = it }, "Credor")
                CampoValor(valorTexto, { valorTexto = it }, "Valor financiado (R$)", invalido = valorTexto.isNotBlank() && valor == null)
                CampoTexto(taxaTexto, { taxaTexto = it }, "Juros (% ao mês)", invalido = taxaTexto.isNotBlank() && taxa == null)
                CampoNumero(parcelasTexto, { parcelasTexto = it }, "Número de parcelas", invalido = parcelasTexto.isNotBlank() && parcelas == null)
                Seletor(
                    rotulo = "Sistema",
                    opcoes = SistemaAmortizacao.entries.toList(),
                    selecionado = sistema,
                    texto = { it.rotulo },
                    aoSelecionar = { sistema = it }
                )
                CampoTexto(primeiraTexto, { primeiraTexto = it }, "Primeira parcela (dd/mm/aaaa)", invalido = primeira == null)

                if (previa == null) {
                    TextoDeApoio("Preencha os campos para ver a simulação. Você pode simular sem salvar.")
                } else {
                    Text("Simulação")
                    LinhaValor("Primeira parcela", previa.primeiraParcela)
                    LinhaValor("Última parcela", previa.ultimaParcela)
                    LinhaValor("Juros totais", previa.jurosTotais)
                    LinhaValor("Custo total", previa.custoTotal, destaque = true)
                    TextoDeApoio("Não inclui seguros, tarifas ou impostos do contrato.")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = credor.isNotBlank() && previa != null,
                onClick = {
                    if (valor != null && taxa != null && parcelas != null && primeira != null) {
                        aoSalvar(credor, valor, taxa, parcelas, sistema, primeira)
                    }
                }
            ) { Text("Salvar dívida") }
        },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Fechar") } }
    )
}

@Composable
fun DialogoPagarParcela(
    item: DividaNaTela,
    contas: List<Conta>,
    aoCancelar: () -> Unit,
    aoPagar: (contaId: Long?) -> Unit
) {
    val parcela = item.resumo.proximaParcela
    val opcoes: List<Conta?> = listOf<Conta?>(null) + contas
    var conta by remember { mutableStateOf(contas.firstOrNull()) }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text("Parcela paga — ${item.divida.credor}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (parcela != null) {
                    LinhaValor("Parcela ${parcela.numero} de ${item.resumo.totalDeParcelas}", parcela.valor)
                    LinhaValor("Juros", parcela.juros)
                    LinhaValor("Amortização", parcela.amortizacao)
                }
                Seletor(
                    rotulo = "Pagar com",
                    opcoes = opcoes,
                    selecionado = conta,
                    texto = { it?.nome ?: "não lançar despesa" },
                    aoSelecionar = { conta = it },
                    textoVazio = "não lançar despesa"
                )
                TextoDeApoio(
                    "Escolhendo uma conta, a parcela é lançada como despesa com a data de hoje. " +
                        "Se você já lançou essa despesa, escolha \"não lançar despesa\" para não contar duas vezes."
                )
            }
        },
        confirmButton = { TextButton(onClick = { aoPagar(conta?.id) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = aoCancelar) { Text("Cancelar") } }
    )
}
