package com.jfpfinance.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jfpfinance.core.Money
import com.jfpfinance.core.model.AporteMeta
import com.jfpfinance.core.model.Bem
import com.jfpfinance.core.model.Divida
import com.jfpfinance.core.model.Investimento
import com.jfpfinance.core.model.Meta
import com.jfpfinance.core.model.MovimentoInvestimento
import com.jfpfinance.core.model.Orcamento
import com.jfpfinance.core.model.SistemaAmortizacao
import com.jfpfinance.core.model.TipoBem
import com.jfpfinance.core.model.TipoInvestimento
import com.jfpfinance.core.model.TipoMeta
import com.jfpfinance.core.model.TipoMovimentoInvestimento
import java.math.BigDecimal
import java.time.LocalDate

/*
 * Datas são guardadas como número de dias (epoch day), valores em centavos e
 * números decimais exatos (taxa, quantidade) como texto, para não perder precisão.
 */

@Entity(tableName = "orcamentos", indices = [Index("categoria_id")])
data class OrcamentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "categoria_id") val categoriaId: Long,
    @ColumnInfo(name = "limite_centavos") val limiteCentavos: Long,
    val ativo: Boolean
)

fun OrcamentoEntity.paraDominio() = Orcamento(id, categoriaId, Money(limiteCentavos), ativo)
fun Orcamento.paraEntidade() = OrcamentoEntity(id, categoriaId, limiteMensal.centavos, ativo)

@Entity(tableName = "metas")
data class MetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val tipo: String,
    @ColumnInfo(name = "valor_alvo_centavos") val valorAlvoCentavos: Long,
    @ColumnInfo(name = "prazo_dia") val prazoDia: Long?,
    val ativa: Boolean,
    @ColumnInfo(name = "criada_em") val criadaEm: Long
)

fun MetaEntity.paraDominio() = Meta(
    id = id,
    nome = nome,
    tipo = TipoMeta.valueOf(tipo),
    valorAlvo = Money(valorAlvoCentavos),
    prazo = prazoDia?.let(LocalDate::ofEpochDay),
    ativa = ativa,
    criadaEm = criadaEm
)

fun Meta.paraEntidade() = MetaEntity(id, nome, tipo.name, valorAlvo.centavos, prazo?.toEpochDay(), ativa, criadaEm)

@Entity(tableName = "meta_aportes", indices = [Index("meta_id")])
data class AporteMetaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "meta_id") val metaId: Long,
    @ColumnInfo(name = "data_dia") val dataDia: Long,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long
)

fun AporteMetaEntity.paraDominio() = AporteMeta(id, metaId, LocalDate.ofEpochDay(dataDia), Money(valorCentavos))
fun AporteMeta.paraEntidade() = AporteMetaEntity(id, metaId, data.toEpochDay(), valor.centavos)

@Entity(tableName = "dividas")
data class DividaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val credor: String,
    @ColumnInfo(name = "valor_original_centavos") val valorOriginalCentavos: Long,
    @ColumnInfo(name = "taxa_mensal_percentual") val taxaMensalPercentual: String,
    val parcelas: Int,
    val sistema: String,
    @ColumnInfo(name = "primeira_parcela_dia") val primeiraParcelaDia: Long,
    @ColumnInfo(name = "parcelas_pagas") val parcelasPagas: Int,
    val ativa: Boolean
)

fun DividaEntity.paraDominio() = Divida(
    id = id,
    credor = credor,
    valorOriginal = Money(valorOriginalCentavos),
    taxaMensalPercentual = BigDecimal(taxaMensalPercentual),
    parcelas = parcelas,
    sistema = SistemaAmortizacao.valueOf(sistema),
    primeiraParcela = LocalDate.ofEpochDay(primeiraParcelaDia),
    parcelasPagas = parcelasPagas,
    ativa = ativa
)

fun Divida.paraEntidade() = DividaEntity(
    id = id,
    credor = credor,
    valorOriginalCentavos = valorOriginal.centavos,
    taxaMensalPercentual = taxaMensalPercentual.toPlainString(),
    parcelas = parcelas,
    sistema = sistema.name,
    primeiraParcelaDia = primeiraParcela.toEpochDay(),
    parcelasPagas = parcelasPagas,
    ativa = ativa
)

@Entity(tableName = "bens")
data class BemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val tipo: String,
    @ColumnInfo(name = "valor_aquisicao_centavos") val valorAquisicaoCentavos: Long,
    @ColumnInfo(name = "data_aquisicao_dia") val dataAquisicaoDia: Long,
    @ColumnInfo(name = "valor_atual_centavos") val valorAtualCentavos: Long,
    val ativo: Boolean
)

fun BemEntity.paraDominio() = Bem(
    id = id,
    nome = nome,
    tipo = TipoBem.valueOf(tipo),
    valorAquisicao = Money(valorAquisicaoCentavos),
    dataAquisicao = LocalDate.ofEpochDay(dataAquisicaoDia),
    valorAtual = Money(valorAtualCentavos),
    ativo = ativo
)

fun Bem.paraEntidade() = BemEntity(
    id, nome, tipo.name, valorAquisicao.centavos, dataAquisicao.toEpochDay(), valorAtual.centavos, ativo
)

@Entity(tableName = "investimentos")
data class InvestimentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val tipo: String,
    @ColumnInfo(name = "valor_atual_centavos") val valorAtualCentavos: Long,
    val ativo: Boolean
)

fun InvestimentoEntity.paraDominio() =
    Investimento(id, nome, TipoInvestimento.valueOf(tipo), Money(valorAtualCentavos), ativo)

fun Investimento.paraEntidade() = InvestimentoEntity(id, nome, tipo.name, valorAtual.centavos, ativo)

@Entity(tableName = "investimento_movimentos", indices = [Index("investimento_id")])
data class MovimentoInvestimentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "investimento_id") val investimentoId: Long,
    val tipo: String,
    @ColumnInfo(name = "data_dia") val dataDia: Long,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long,
    val quantidade: String?,
    @ColumnInfo(name = "custo_baixado_centavos") val custoBaixadoCentavos: Long
)

fun MovimentoInvestimentoEntity.paraDominio() = MovimentoInvestimento(
    id = id,
    investimentoId = investimentoId,
    tipo = TipoMovimentoInvestimento.valueOf(tipo),
    data = LocalDate.ofEpochDay(dataDia),
    valor = Money(valorCentavos),
    quantidade = quantidade?.let(::BigDecimal),
    custoBaixado = Money(custoBaixadoCentavos)
)

fun MovimentoInvestimento.paraEntidade() = MovimentoInvestimentoEntity(
    id = id,
    investimentoId = investimentoId,
    tipo = tipo.name,
    dataDia = data.toEpochDay(),
    valorCentavos = valor.centavos,
    quantidade = quantidade?.toPlainString(),
    custoBaixadoCentavos = custoBaixado.centavos
)
