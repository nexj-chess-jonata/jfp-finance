package com.jfpfinance.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Cartao
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.Pessoa
import com.jfpfinance.core.model.TipoCategoria

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val tipo: String,
    @ColumnInfo(name = "pai_id") val paiId: Long?,
    val ativa: Boolean
)

fun CategoriaEntity.paraDominio(): Categoria = Categoria(
    id = id,
    nome = nome,
    tipo = TipoCategoria.valueOf(tipo),
    paiId = paiId,
    ativa = ativa
)

fun Categoria.paraEntidade(): CategoriaEntity = CategoriaEntity(
    id = id,
    nome = nome,
    tipo = tipo.name,
    paiId = paiId,
    ativa = ativa
)

@Entity(tableName = "pessoas")
data class PessoaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val ativa: Boolean
)

fun PessoaEntity.paraDominio(): Pessoa = Pessoa(id = id, nome = nome, ativa = ativa)

fun Pessoa.paraEntidade(): PessoaEntity = PessoaEntity(id = id, nome = nome, ativa = ativa)

@Entity(tableName = "cartoes")
data class CartaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    @ColumnInfo(name = "limite_centavos") val limiteCentavos: Long,
    @ColumnInfo(name = "dia_fechamento") val diaFechamento: Int,
    @ColumnInfo(name = "dia_vencimento") val diaVencimento: Int,
    val moeda: String,
    val ativo: Boolean
)

fun CartaoEntity.paraDominio(): Cartao = Cartao(
    id = id,
    nome = nome,
    limite = Money(limiteCentavos),
    diaFechamento = diaFechamento,
    diaVencimento = diaVencimento,
    moeda = moeda,
    ativo = ativo
)

fun Cartao.paraEntidade(): CartaoEntity = CartaoEntity(
    id = id,
    nome = nome,
    limiteCentavos = limite.centavos,
    diaFechamento = diaFechamento,
    diaVencimento = diaVencimento,
    moeda = moeda,
    ativo = ativo
)
