package com.jfpfinance.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jfpfinance.core.Money
import com.jfpfinance.core.model.Conta
import com.jfpfinance.core.model.TipoConta

@Entity(tableName = "contas")
data class ContaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val tipo: String,
    @ColumnInfo(name = "saldo_inicial_centavos") val saldoInicialCentavos: Long,
    val moeda: String,
    val ativa: Boolean,
    @ColumnInfo(name = "criada_em") val criadaEm: Long
)

fun ContaEntity.paraDominio(): Conta = Conta(
    id = id,
    nome = nome,
    tipo = runCatching { TipoConta.valueOf(tipo) }.getOrDefault(TipoConta.OUTRA),
    saldoInicial = Money(saldoInicialCentavos),
    moeda = moeda,
    ativa = ativa,
    criadaEm = criadaEm
)

fun Conta.paraEntidade(): ContaEntity = ContaEntity(
    id = id,
    nome = nome,
    tipo = tipo.name,
    saldoInicialCentavos = saldoInicial.centavos,
    moeda = moeda,
    ativa = ativa,
    criadaEm = criadaEm
)
