package com.jfpfinance.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Operacao
import com.jfpfinance.core.model.OrigemOperacao
import com.jfpfinance.core.model.SituacaoOperacao
import com.jfpfinance.core.model.TipoOperacao
import java.time.LocalDate

/** Linha do livro financeiro. A data é guardada como número de dias (epoch day). */
@Entity(
    tableName = "operacoes",
    indices = [Index("data_dia"), Index("conta_id"), Index("cartao_id"), Index("reverte_id")]
)
data class OperacaoEntity(
    @PrimaryKey val id: String,
    val tipo: String,
    @ColumnInfo(name = "data_dia") val dataDia: Long,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long,
    val moeda: String,
    @ColumnInfo(name = "conta_id") val contaId: Long?,
    @ColumnInfo(name = "conta_destino_id") val contaDestinoId: Long?,
    @ColumnInfo(name = "cartao_id") val cartaoId: Long?,
    @ColumnInfo(name = "categoria_id") val categoriaId: Long?,
    val descricao: String,
    val contraparte: String?,
    val origem: String,
    val situacao: String,
    @ColumnInfo(name = "reverte_id") val reverteId: String?,
    @ColumnInfo(name = "grupo_id") val grupoId: String?,
    val parcela: Int?,
    @ColumnInfo(name = "total_parcelas") val totalParcelas: Int?,
    @ColumnInfo(name = "pessoa_id") val pessoaId: Long?,
    @ColumnInfo(name = "origem_detalhe") val origemDetalhe: String?,
    @ColumnInfo(name = "criada_em") val criadaEm: Long
)

fun OperacaoEntity.paraDominio(): Operacao = Operacao(
    id = id,
    tipo = TipoOperacao.valueOf(tipo),
    data = LocalDate.ofEpochDay(dataDia),
    valor = Money(valorCentavos),
    moeda = moeda,
    contaId = contaId,
    contaDestinoId = contaDestinoId,
    cartaoId = cartaoId,
    categoriaId = categoriaId,
    descricao = descricao,
    contraparte = contraparte,
    origem = OrigemOperacao.valueOf(origem),
    situacao = SituacaoOperacao.valueOf(situacao),
    reverteId = reverteId,
    grupoId = grupoId,
    parcela = parcela,
    totalParcelas = totalParcelas,
    pessoaId = pessoaId,
    origemDetalhe = origemDetalhe,
    criadaEm = criadaEm
)

fun Operacao.paraEntidade(): OperacaoEntity = OperacaoEntity(
    id = id,
    tipo = tipo.name,
    dataDia = data.toEpochDay(),
    valorCentavos = valor.centavos,
    moeda = moeda,
    contaId = contaId,
    contaDestinoId = contaDestinoId,
    cartaoId = cartaoId,
    categoriaId = categoriaId,
    descricao = descricao,
    contraparte = contraparte,
    origem = origem.name,
    situacao = situacao.name,
    reverteId = reverteId,
    grupoId = grupoId,
    parcela = parcela,
    totalParcelas = totalParcelas,
    pessoaId = pessoaId,
    origemDetalhe = origemDetalhe,
    criadaEm = criadaEm
)
