package com.jfpfinance.app.ui.dados

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jfpfinance.backup.BackupInvalidoException
import com.jfpfinance.core.model.Pessoa
import com.jfpfinance.core.repository.PessoaRepository
import com.jfpfinance.engine.GestaoFinanceira
import com.jfpfinance.engine.LeitorDeDados
import com.jfpfinance.integrations.Arquivos
import com.jfpfinance.reports.Exportadores
import com.jfpfinance.reports.Relatorios
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DadosViewModel(
    leitor: LeitorDeDados,
    private val gestao: GestaoFinanceira,
    private val pessoaRepository: PessoaRepository,
    private val gerenciador: GerenciadorDeDados
) : ViewModel() {

    private val local = MutableStateFlow(DadosUiState())

    val estado: StateFlow<DadosUiState> =
        combine(leitor.observar(), local) { dados, atual -> atual.copy(dados = dados) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DadosUiState())

    fun limparMensagem() {
        local.value = local.value.copy(mensagem = null)
    }

    // ---------- Importação ----------

    fun abrirExtrato(uri: Uri) = executar {
        val dados = estado.value.dados ?: return@executar
        val conta = dados.contas.firstOrNull()
            ?: throw IllegalArgumentException("Cadastre uma conta na tela Início antes de importar.")
        val arquivo = gerenciador.ler(uri)
        val leitura = Arquivos.ler(arquivo.nome, arquivo.bytes)
        if (leitura.lancamentos.isEmpty()) {
            throw IllegalArgumentException(
                (listOf("Nenhum lançamento pôde ser lido de ${arquivo.nome}.") + leitura.avisos.take(3)).joinToString("\n")
            )
        }
        local.value = local.value.copy(
            importacao = iniciarImportacao(
                arquivo.nome, leitura.lancamentos, leitura.avisos, conta.id, dados, System.currentTimeMillis()
            )
        )
    }

    fun escolherContaDaImportacao(contaId: Long) {
        val dados = estado.value.dados ?: return
        val importacao = local.value.importacao ?: return
        local.value = local.value.copy(importacao = importacao.comConta(contaId, dados, System.currentTimeMillis()))
    }

    fun alternarItem(indice: Int) {
        local.value = local.value.copy(importacao = local.value.importacao?.alternando(indice))
    }

    fun definirCategoria(indice: Int, categoriaId: Long?) {
        local.value = local.value.copy(importacao = local.value.importacao?.comCategoria(indice, categoriaId))
    }

    fun cancelarImportacao() {
        local.value = local.value.copy(importacao = null)
    }

    /** Grava os selecionados pelo motor: todos ou nenhum. */
    fun confirmarImportacao() = executar {
        val selecionados = local.value.importacao?.selecionados.orEmpty()
        if (selecionados.isEmpty()) throw IllegalArgumentException("Nenhum lançamento selecionado.")
        gestao.registrar(selecionados)
        local.value = local.value.copy(
            importacao = null,
            mensagem = "${selecionados.size} lançamento(s) importado(s)."
        )
    }

    // ---------- Relatórios ----------

    fun gerarRelatorio(inicio: LocalDate, fim: LocalDate, filtrarPorPessoa: Boolean, pessoaId: Long?) = executar {
        val dados = estado.value.dados ?: return@executar
        local.value = local.value.copy(relatorio = Relatorios.doPeriodo(dados, inicio, fim, pessoaId, filtrarPorPessoa))
    }

    fun salvarRelatorio(uri: Uri, formato: FormatoDeRelatorio) = executar {
        val relatorio = local.value.relatorio ?: return@executar
        val bytes = when (formato) {
            FormatoDeRelatorio.PDF -> gerenciador.pdf(Exportadores.texto(relatorio))
            FormatoDeRelatorio.CSV -> Exportadores.csv(relatorio).toByteArray(Charsets.UTF_8)
            FormatoDeRelatorio.JSON -> Exportadores.json(relatorio).toByteArray(Charsets.UTF_8)
        }
        gerenciador.escrever(uri, bytes)
        local.value = local.value.copy(mensagem = "Relatório salvo.")
    }

    // ---------- Família ----------

    fun adicionarPessoa(nome: String) = executar {
        if (nome.isBlank()) throw IllegalArgumentException("Informe o nome.")
        pessoaRepository.salvar(Pessoa(nome = nome.trim()))
    }

    fun arquivarPessoa(id: Long) = executar { pessoaRepository.arquivar(id) }

    // ---------- Backup e exclusão ----------

    fun criarBackup(destino: Uri, senha: String) = executar {
        gerenciador.criarBackup(destino, senha.toCharArray())
        local.value = local.value.copy(mensagem = "Backup criado e conferido. Guarde a senha: sem ela o backup não abre.")
    }

    fun restaurar(origem: Uri, senha: String) = executar {
        gerenciador.restaurar(origem, senha.toCharArray())
        // O banco em uso foi fechado e trocado: reinicia na hora, antes que alguma tela tente lê-lo.
        gerenciador.reiniciar()
    }

    fun apagarTudo() = executar {
        gerenciador.apagarTudo()
        gerenciador.reiniciar()
    }

    private fun executar(acao: suspend () -> Unit) {
        viewModelScope.launch {
            local.value = local.value.copy(ocupado = true)
            try {
                acao()
            } catch (e: IllegalArgumentException) {
                local.value = local.value.copy(mensagem = e.message ?: "Não foi possível concluir a operação.")
            } catch (e: BackupInvalidoException) {
                local.value = local.value.copy(mensagem = e.message)
            } catch (e: IOException) {
                local.value = local.value.copy(mensagem = e.message ?: "Erro ao ler ou gravar o arquivo.")
            } catch (e: SecurityException) {
                local.value = local.value.copy(mensagem = "Sem permissão para acessar esse arquivo.")
            } finally {
                local.value = local.value.copy(ocupado = false)
            }
        }
    }

    companion object {
        fun fabrica(
            leitor: LeitorDeDados,
            gestao: GestaoFinanceira,
            pessoaRepository: PessoaRepository,
            gerenciador: GerenciadorDeDados
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { DadosViewModel(leitor, gestao, pessoaRepository, gerenciador) }
        }
    }
}

enum class FormatoDeRelatorio(val rotulo: String, val mime: String, val extensao: String) {
    PDF("PDF", "application/pdf", "pdf"),
    CSV("CSV (Excel)", "text/csv", "csv"),
    JSON("JSON", "application/json", "json")
}
