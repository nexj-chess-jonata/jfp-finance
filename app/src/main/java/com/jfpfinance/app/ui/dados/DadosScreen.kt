package com.jfpfinance.app.ui.dados

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jfpfinance.backup.CofreDeBackup
import com.jfpfinance.core.formatarBR
import com.jfpfinance.core.lerDataBR
import com.jfpfinance.core.model.Categoria
import com.jfpfinance.core.model.TipoCategoria
import com.jfpfinance.core.model.TipoOperacao
import com.jfpfinance.integrations.SituacaoDoItem
import com.jfpfinance.reports.Exportadores
import com.jfpfinance.ui.comum.Abas
import com.jfpfinance.ui.comum.CampoTexto
import com.jfpfinance.ui.comum.DialogoDeConfirmacao
import com.jfpfinance.ui.comum.Seletor
import com.jfpfinance.ui.comum.TextoDeApoio
import java.time.YearMonth

private val ABAS = listOf("Importar", "Relatório", "Backup", "Família")
private const val LINHAS_DA_PREVIA = 60

@Composable
fun DadosScreen(viewModel: DadosViewModel, aoVoltar: () -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = aoVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text("Dados", style = MaterialTheme.typography.titleLarge)
        }
        Abas(titulos = ABAS, selecionada = aba, aoSelecionar = { aba = it })
        if (estado.ocupado) TextoDeApoio("  Trabalhando…")
        when (aba) {
            0 -> AbaImportar(estado, viewModel)
            1 -> AbaRelatorio(estado, viewModel)
            2 -> AbaBackup(estado, viewModel)
            else -> AbaFamilia(estado, viewModel)
        }
    }

    estado.mensagem?.let { mensagem ->
        AlertDialog(
            onDismissRequest = viewModel::limparMensagem,
            text = { Text(mensagem) },
            confirmButton = { TextButton(onClick = viewModel::limparMensagem) { Text("OK") } }
        )
    }
}

// ------------------------------------------------------------------ Importar

@Composable
private fun AbaImportar(estado: DadosUiState, viewModel: DadosViewModel) {
    val escolherArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.abrirExtrato(uri)
    }
    val importacao = estado.importacao
    val dados = estado.dados

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (importacao == null || dados == null) {
            item {
                TextoDeApoio(
                    "Importe o extrato que você baixou do banco, em OFX ou CSV. Você confere tudo antes de " +
                        "gravar, e importar o mesmo arquivo duas vezes não duplica lançamentos. " +
                        "Planilhas do Excel (XLSX) ainda não são lidas: salve como CSV."
                )
            }
            item {
                Button(onClick = { escolherArquivo.launch(arrayOf("*/*")) }, enabled = !estado.ocupado) {
                    Text("Escolher arquivo")
                }
            }
        } else {
            item {
                Text(importacao.nomeDoArquivo, style = MaterialTheme.typography.titleMedium)
                TextoDeApoio("${importacao.itens.size} lançamento(s) lido(s). Nada foi gravado ainda.")
            }
            item {
                Seletor(
                    rotulo = "Conta do extrato",
                    opcoes = dados.contas,
                    selecionado = dados.contas.firstOrNull { it.id == importacao.contaId },
                    texto = { it.nome },
                    aoSelecionar = { viewModel.escolherContaDaImportacao(it.id) }
                )
            }
            if (importacao.avisos.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Avisos da leitura (${importacao.avisos.size})", style = MaterialTheme.typography.labelLarge)
                            importacao.avisos.take(10).forEach { TextoDeApoio(it, erro = true) }
                            if (importacao.avisos.size > 10) TextoDeApoio("… e mais ${importacao.avisos.size - 10}.")
                        }
                    }
                }
            }
            item {
                TextoDeApoio(
                    "Transferências entre suas contas e pagamentos de fatura aparecem no extrato como saída comum. " +
                        "Desmarque essas linhas e lance pelo aplicativo, para não contarem como despesa."
                )
            }
            itemsIndexed(importacao.itens) { indice, item ->
                val ehReceita = item.operacao.tipo == TipoOperacao.RECEITA
                val categorias = dados.categorias.filter {
                    it.tipo == if (ehReceita) TipoCategoria.RECEITA else TipoCategoria.DESPESA
                }
                val opcoes: List<Categoria?> = listOf<Categoria?>(null) + categorias
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.selecionado, onCheckedChange = { viewModel.alternarItem(indice) })
                            Column(Modifier.weight(1f)) {
                                Text(item.operacao.descricao.ifBlank { "(sem descrição)" }, style = MaterialTheme.typography.bodyMedium)
                                TextoDeApoio(item.operacao.data.formatarBR())
                            }
                            Text(
                                (if (ehReceita) "+ " else "- ") + item.operacao.valor.formatarBRL(),
                                fontWeight = FontWeight.Medium,
                                color = if (ehReceita) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        if (item.situacao != SituacaoDoItem.NOVO) TextoDeApoio(item.situacao.rotulo, erro = true)
                        Seletor(
                            rotulo = "Categoria",
                            opcoes = opcoes,
                            selecionado = categorias.firstOrNull { it.id == item.operacao.categoriaId },
                            texto = { it?.nome ?: "sem categoria" },
                            aoSelecionar = { viewModel.definirCategoria(indice, it?.id) },
                            textoVazio = "sem categoria"
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = viewModel::confirmarImportacao,
                        enabled = importacao.selecionados.isNotEmpty() && !estado.ocupado
                    ) { Text("Importar ${importacao.selecionados.size}") }
                    OutlinedButton(onClick = viewModel::cancelarImportacao) { Text("Cancelar") }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Relatório

@Composable
private fun AbaRelatorio(estado: DadosUiState, viewModel: DadosViewModel) {
    val dados = estado.dados
    val hoje = dados?.hoje ?: java.time.LocalDate.now()
    var inicioTexto by remember { mutableStateOf(YearMonth.from(hoje).atDay(1).formatarBR()) }
    var fimTexto by remember { mutableStateOf(hoje.formatarBR()) }
    // 0 = todos; 1 = só lançamentos da casa (sem pessoa); 2 em diante = cada pessoa.
    var filtro by remember { mutableIntStateOf(0) }
    val pessoas = dados?.pessoas.orEmpty()
    val opcoesDeFiltro = listOf("todos", "só da casa (sem pessoa)") + pessoas.map { it.nome }

    val inicio = lerDataBR(inicioTexto)
    val fim = lerDataBR(fimTexto)
    val periodoValido = inicio != null && fim != null && !fim.isBefore(inicio)
    val relatorio = estado.relatorio

    var formato by remember { mutableStateOf(FormatoDeRelatorio.PDF) }
    val salvarPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        if (uri != null) viewModel.salvarRelatorio(uri, FormatoDeRelatorio.PDF)
    }
    val salvarCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) viewModel.salvarRelatorio(uri, FormatoDeRelatorio.CSV)
    }
    val salvarJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) viewModel.salvarRelatorio(uri, FormatoDeRelatorio.JSON)
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { CampoTexto(inicioTexto, { inicioTexto = it }, "Início (dd/mm/aaaa)", invalido = inicio == null) }
        item { CampoTexto(fimTexto, { fimTexto = it }, "Fim (dd/mm/aaaa)", invalido = !periodoValido) }
        if (pessoas.isNotEmpty()) {
            item {
                Seletor(
                    rotulo = "Lançamentos de",
                    opcoes = opcoesDeFiltro.indices.toList(),
                    selecionado = filtro,
                    texto = { opcoesDeFiltro[it] },
                    aoSelecionar = { filtro = it }
                )
            }
        }
        item {
            Button(
                enabled = periodoValido && dados != null,
                onClick = {
                    if (inicio != null && fim != null) {
                        viewModel.gerarRelatorio(
                            inicio = inicio,
                            fim = fim,
                            filtrarPorPessoa = filtro > 0,
                            pessoaId = if (filtro >= 2) pessoas.getOrNull(filtro - 2)?.id else null
                        )
                    }
                }
            ) { Text("Gerar relatório") }
        }
        if (relatorio != null) {
            item {
                Seletor(
                    rotulo = "Formato",
                    opcoes = FormatoDeRelatorio.entries.toList(),
                    selecionado = formato,
                    texto = { it.rotulo },
                    aoSelecionar = { formato = it }
                )
            }
            item {
                OutlinedButton(
                    enabled = !estado.ocupado,
                    onClick = {
                        val nome = "jfp-finance-${relatorio.inicio}-a-${relatorio.fim}.${formato.extensao}"
                        when (formato) {
                            FormatoDeRelatorio.PDF -> salvarPdf.launch(nome)
                            FormatoDeRelatorio.CSV -> salvarCsv.launch(nome)
                            FormatoDeRelatorio.JSON -> salvarJson.launch(nome)
                        }
                    }
                ) { Text("Salvar arquivo") }
            }
            item {
                TextoDeApoio(
                    "O arquivo salvo sai do aplicativo sem proteção: quem tiver o arquivo consegue ler. " +
                        "O CSV e o JSON trazem só os lançamentos e o cabeçalho; o PDF traz o relatório inteiro."
                )
            }
            val linhas = Exportadores.texto(relatorio)
            items(linhas.take(LINHAS_DA_PREVIA)) { linha -> Text(linha, style = MaterialTheme.typography.bodySmall) }
            if (linhas.size > LINHAS_DA_PREVIA) {
                item { TextoDeApoio("… mais ${linhas.size - LINHAS_DA_PREVIA} linha(s) no arquivo salvo.") }
            }
        }
    }
}

// ------------------------------------------------------------------ Backup

@Composable
private fun AbaBackup(estado: DadosUiState, viewModel: DadosViewModel) {
    var senha by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var arquivoParaRestaurar by remember { mutableStateOf<Uri?>(null) }
    var mostrarApagar by remember { mutableStateOf(false) }

    val senhaValida = senha.length >= CofreDeBackup.SENHA_MINIMA && senha == confirmacao
    val criar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null) {
            viewModel.criarBackup(uri, senha)
            senha = ""
            confirmacao = ""
        }
    }
    val escolherBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        arquivoParaRestaurar = uri
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Text("Criar backup", style = MaterialTheme.typography.titleMedium) }
        item {
            TextoDeApoio(
                "Inclui tudo o que está no banco: contas, lançamentos, categorias, cartões, orçamentos, metas, " +
                    "dívidas, bens, investimentos e pessoas. Não inclui o PIN nem as preferências do aplicativo. " +
                    "O arquivo é criptografado com a senha abaixo. Sem a senha não há como abrir o backup."
            )
        }
        item { CampoSenha(senha, { senha = it }, "Senha do backup (mínimo ${CofreDeBackup.SENHA_MINIMA} caracteres)") }
        item { CampoSenha(confirmacao, { confirmacao = it }, "Repita a senha", invalido = confirmacao.isNotEmpty() && senha != confirmacao) }
        item {
            Button(
                enabled = senhaValida && !estado.ocupado,
                onClick = { criar.launch("jfp-finance-${estado.dados?.hoje ?: ""}.jfpbkp") }
            ) { Text("Criar backup") }
        }
        item { TextoDeApoio("O backup é manual: não há cópia automática nem em nuvem. Você escolhe onde salvar o arquivo.") }

        item { Text("Restaurar backup", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        item {
            TextoDeApoio(
                "Restaurar substitui todos os dados atuais pelos do backup. O banco atual fica guardado no aparelho " +
                    "como cópia de segurança até a próxima restauração."
            )
        }
        item {
            OutlinedButton(onClick = { escolherBackup.launch(arrayOf("*/*")) }, enabled = !estado.ocupado) {
                Text("Escolher arquivo de backup")
            }
        }

        item { Text("Apagar todos os dados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        item {
            OutlinedButton(onClick = { mostrarApagar = true }, enabled = !estado.ocupado) {
                Text("Apagar tudo", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    arquivoParaRestaurar?.let { uri ->
        var senhaDoBackup by remember(uri) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { arquivoParaRestaurar = null },
            title = { Text("Restaurar backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Todos os dados atuais serão substituídos pelos do backup, e o aplicativo será reiniciado.")
                    CampoSenha(senhaDoBackup, { senhaDoBackup = it }, "Senha do backup")
                    TextoDeApoio("Com a senha errada ou o arquivo alterado, nada é substituído.")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = senhaDoBackup.isNotEmpty(),
                    onClick = {
                        viewModel.restaurar(uri, senhaDoBackup)
                        arquivoParaRestaurar = null
                    }
                ) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { arquivoParaRestaurar = null }) { Text("Cancelar") } }
        )
    }

    if (mostrarApagar) {
        var palavra by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { mostrarApagar = false },
            title = { Text("Apagar todos os dados") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Do aparelho serão apagados: o banco de dados inteiro, a cópia de segurança da última " +
                            "restauração, o PIN e as preferências. Isso não pode ser desfeito."
                    )
                    Text("Em servidores: nada, porque o aplicativo não envia dados para nenhum servidor.")
                    Text(
                        "Backups e relatórios que você salvou em arquivos não são apagados por aqui. " +
                            "Apague esses arquivos você mesmo, se quiser."
                    )
                    CampoTexto(palavra, { palavra = it }, "Digite APAGAR para confirmar")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = palavra.trim() == "APAGAR",
                    onClick = {
                        mostrarApagar = false
                        viewModel.apagarTudo()
                    }
                ) { Text("Apagar tudo", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { mostrarApagar = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CampoSenha(valor: String, aoMudar: (String) -> Unit, rotulo: String, invalido: Boolean = false) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        isError = invalido,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}

// ------------------------------------------------------------------ Família

@Composable
private fun AbaFamilia(estado: DadosUiState, viewModel: DadosViewModel) {
    var nome by remember { mutableStateOf("") }
    var arquivar by remember { mutableStateOf<com.jfpfinance.core.model.Pessoa?>(null) }
    val pessoas = estado.dados?.pessoas.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            TextoDeApoio(
                "Cadastre as pessoas da casa para indicar de quem é cada lançamento e gerar relatórios por pessoa. " +
                    "Tudo fica neste aparelho: ainda não existe grupo familiar conectado entre celulares nem " +
                    "permissões por pessoa. Quem abre o aplicativo vê todos os dados."
            )
        }
        item { CampoTexto(nome, { nome = it }, "Nome da pessoa") }
        item {
            Button(
                enabled = nome.isNotBlank(),
                onClick = {
                    viewModel.adicionarPessoa(nome)
                    nome = ""
                }
            ) { Text("Adicionar") }
        }
        if (pessoas.isEmpty()) {
            item { TextoDeApoio("Nenhuma pessoa cadastrada. Sem pessoas, todos os lançamentos são da casa.") }
        }
        items(pessoas, key = { it.id }) { pessoa ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(pessoa.nome, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { arquivar = pessoa }) { Text("Arquivar") }
                }
            }
        }
    }

    arquivar?.let { pessoa ->
        DialogoDeConfirmacao(
            titulo = "Arquivar pessoa",
            mensagem = "Arquivar ${pessoa.nome}? Os lançamentos dela continuam no histórico.",
            aoConfirmar = {
                viewModel.arquivarPessoa(pessoa.id)
                arquivar = null
            },
            aoCancelar = { arquivar = null }
        )
    }
}
