package com.kenji.rotisseria00.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kenji.rotisseria00.ui.screens.AdminScreen
import com.kenji.rotisseria00.ui.screens.CaixaScreen
import com.kenji.rotisseria00.ui.screens.CardapioSalaoScreen
import com.kenji.rotisseria00.ui.screens.LoginScreen
import com.kenji.rotisseria00.ui.screens.MesasScreen
import com.kenji.rotisseria00.ui.screens.ComandaScreen // Import da nossa tela nova!
import com.kenji.rotisseria00.ui.screens.CozinhaScreen

@Composable
fun AppNavigation(
    onStartService: () -> Unit = {},
    onStopService: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        // Rota 1: Login
        composable("login") {
            LoginScreen(
                onLoginSucesso = { perfil ->
                    when (perfil) {
                        "ADMIN" -> {
                            onStartService()
                            navController.navigate("admin") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        "COZINHA" -> {
                            onStopService() // Cozinha não precisa do serviço de segundo plano do garçom
                            navController.navigate("cozinha") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                        else -> {
                            onStartService()
                            navController.navigate("mesas") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // Rota 2: Mesas
        composable("mesas") {
            MesasScreen(
                // Quando clicar numa mesa, dizemos ao navegador para abrir a comanda correspondente
                onMesaClick = { numeroMesa ->
                    navController.navigate("comanda/$numeroMesa")
                }
            )
        }

        // Rota 3: Comanda Específica (Recebe o número da mesa na URL)
        composable(
            route = "comanda/{numeroMesa}",
            arguments = listOf(navArgument("numeroMesa") { type = NavType.StringType })
        ) { backStackEntry ->
            // Extrai o número da mesa que foi passado
            val numeroMesa = backStackEntry.arguments?.getString("numeroMesa") ?: "00"

            ComandaScreen(
                numeroMesa = numeroMesa,
                onVoltar = {
                    navController.popBackStack() // Acão para voltar à tela de mesas
                }
            )
        }

        // Rota 4: Dashboard
        composable("dashboard") {
            CaixaScreen()
        }

        // Rota do Painel Completo do Tablet
        composable("admin") {
            AdminScreen()
        }

        composable("salao") {
            CardapioSalaoScreen() // A tela nova que você acabou de criar!
        }

        composable("cozinha") {
            CozinhaScreen()
        }
    }
}