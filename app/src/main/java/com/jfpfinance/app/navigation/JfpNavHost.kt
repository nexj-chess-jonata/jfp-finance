package com.jfpfinance.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jfpfinance.app.Dependencias
import com.jfpfinance.app.ui.dados.DadosScreen
import com.jfpfinance.app.ui.dados.DadosViewModel
import com.jfpfinance.app.ui.dados.GerenciadorDeDados
import com.jfpfinance.app.ui.seguranca.ConfiguracoesScreen
import com.jfpfinance.ui.assistente.AssistenteScreen
import com.jfpfinance.ui.assistente.AssistenteViewModel
import com.jfpfinance.ui.dashboard.DashboardScreen
import com.jfpfinance.ui.dashboard.DashboardViewModel
import com.jfpfinance.ui.movimentacoes.MovimentacoesScreen
import com.jfpfinance.ui.movimentacoes.MovimentacoesViewModel
import com.jfpfinance.ui.patrimonio.PatrimonioScreen
import com.jfpfinance.ui.patrimonio.PatrimonioViewModel
import com.jfpfinance.ui.planejamento.PlanejamentoScreen
import com.jfpfinance.ui.planejamento.PlanejamentoViewModel

/** As cinco áreas principais do aplicativo (seção 5 do Prompt-Mestre). */
enum class AreaPrincipal(val rota: String, val rotulo: String, val icone: ImageVector) {
    INICIO("inicio", "Início", Icons.Filled.Home),
    MOVIMENTACOES("movimentacoes", "Movimentações", Icons.Filled.SwapHoriz),
    PLANEJAMENTO("planejamento", "Planejamento", Icons.Filled.DateRange),
    PATRIMONIO("patrimonio", "Patrimônio", Icons.Filled.AccountBalance),
    ASSISTENTE("assistente", "Assistente", Icons.Filled.SmartToy)
}

private const val ROTA_CONFIGURACOES = "configuracoes"
private const val ROTA_DADOS = "dados"

@Composable
fun JfpNavHost(dependencias: Dependencias) {
    val navController = rememberNavController()
    val entradaAtual by navController.currentBackStackEntryAsState()
    val rotaAtual = entradaAtual?.destination?.route

    Scaffold(
        bottomBar = {
            if (rotaAtual != ROTA_CONFIGURACOES && rotaAtual != ROTA_DADOS) {
                NavigationBar {
                    AreaPrincipal.entries.forEach { area ->
                        NavigationBarItem(
                            selected = rotaAtual == area.rota,
                            onClick = {
                                navController.navigate(area.rota) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(area.icone, contentDescription = null) },
                            label = { Text(area.rotulo, maxLines = 1) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    ) { espacos ->
        NavHost(
            navController = navController,
            startDestination = AreaPrincipal.INICIO.rota,
            modifier = Modifier.padding(espacos)
        ) {
            composable(AreaPrincipal.INICIO.rota) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.fabrica(
                        dependencias.contaRepository,
                        dependencias.operacaoRepository
                    )
                )
                DashboardScreen(
                    viewModel = viewModel,
                    aoAbrirConfiguracoes = { navController.navigate(ROTA_CONFIGURACOES) }
                )
            }
            composable(AreaPrincipal.MOVIMENTACOES.rota) {
                val viewModel: MovimentacoesViewModel = viewModel(
                    factory = MovimentacoesViewModel.fabrica(
                        dependencias.contaRepository,
                        dependencias.categoriaRepository,
                        dependencias.cartaoRepository,
                        dependencias.operacaoRepository,
                        dependencias.pessoaRepository,
                        dependencias.gestaoFinanceira
                    )
                )
                MovimentacoesScreen(viewModel = viewModel)
            }
            composable(AreaPrincipal.PLANEJAMENTO.rota) {
                val viewModel: PlanejamentoViewModel = viewModel(
                    factory = PlanejamentoViewModel.fabrica(
                        dependencias.contaRepository,
                        dependencias.categoriaRepository,
                        dependencias.orcamentoRepository,
                        dependencias.metaRepository,
                        dependencias.dividaRepository,
                        dependencias.operacaoRepository,
                        dependencias.gestaoFinanceira
                    )
                )
                PlanejamentoScreen(viewModel = viewModel)
            }
            composable(AreaPrincipal.PATRIMONIO.rota) {
                val viewModel: PatrimonioViewModel = viewModel(
                    factory = PatrimonioViewModel.fabrica(
                        dependencias.contaRepository,
                        dependencias.cartaoRepository,
                        dependencias.dividaRepository,
                        dependencias.bemRepository,
                        dependencias.investimentoRepository,
                        dependencias.operacaoRepository,
                        dependencias.gestaoFinanceira
                    )
                )
                PatrimonioScreen(viewModel = viewModel)
            }
            composable(AreaPrincipal.ASSISTENTE.rota) {
                val viewModel: AssistenteViewModel = viewModel(
                    factory = AssistenteViewModel.fabrica(dependencias.leitorDeDados)
                )
                AssistenteScreen(viewModel = viewModel)
            }
            composable(ROTA_CONFIGURACOES) {
                ConfiguracoesScreen(
                    pinManager = dependencias.pinManager,
                    preferencias = dependencias.preferencias,
                    aoAbrirDados = { navController.navigate(ROTA_DADOS) },
                    aoVoltar = { navController.popBackStack() }
                )
            }
            composable(ROTA_DADOS) {
                val context = LocalContext.current
                val viewModel: DadosViewModel = viewModel(
                    factory = DadosViewModel.fabrica(
                        dependencias.leitorDeDados,
                        dependencias.gestaoFinanceira,
                        dependencias.pessoaRepository,
                        GerenciadorDeDados(context)
                    )
                )
                DadosScreen(viewModel = viewModel, aoVoltar = { navController.popBackStack() })
            }
        }
    }
}
