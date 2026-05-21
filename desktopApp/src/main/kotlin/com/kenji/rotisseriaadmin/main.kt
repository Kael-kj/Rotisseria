package com.kenji.rotisseriaadmin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.kenji.rotisseriaadmin.ui.screens.CaixaScreen
import com.kenji.rotisseriaadmin.ui.screens.CardapioScreen
import com.kenji.rotisseriaadmin.ui.screens.DashboardScreen
import com.kenji.rotisseriaadmin.ui.screens.EstoqueScreen
import com.kenji.rotisseriaadmin.ui.screens.FiadosScreen
import com.kenji.rotisseriaadmin.ui.screens.HistoricoScreen
import com.kenji.rotisseriaadmin.ui.screens.NovoPedidoScreen
import com.kenji.rotisseriaadmin.ui.theme.*

@Composable
fun WindowScope.NuiApp(windowState: WindowState, onClose: () -> Unit) {
    var telaAtual by remember { mutableStateOf("Centro de Comando") }

    Rotisseria00Theme {
        Column(modifier = Modifier.fillMaxSize().background(BackgroundCream)) {

            // ====================================================
            // BARRA DE TOPO PERSONALIZADA (DRAGGABLE)
            // ====================================================
            WindowDraggableArea {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(PrimaryBrown),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ROTISSERIA ADMIN",
                        color = OnPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Row {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(48.dp)
                                .clickable { windowState.placement = WindowPlacement.Maximized },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗖", color = OnPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(48.dp)
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = OnPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ====================================================
            // CORPO DA APLICAÇÃO
            // ====================================================
            Row(Modifier.fillMaxSize()) {

                // BARRA LATERAL (MENU)
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .background(PrimaryBrown)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Rotisseria", color = OnPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }

                    NuiMenuButton("Centro de Comando", Icons.Default.Dashboard, telaAtual) { telaAtual = "Centro de Comando" }
                    NuiMenuButton("Novo Pedido", Icons.Default.AddShoppingCart, telaAtual) { telaAtual = "Novo Pedido" }
                    NuiMenuButton("Caixa", Icons.Default.PointOfSale, telaAtual) { telaAtual = "Caixa" }
                    NuiMenuButton("Fiados", Icons.Default.MoneyOff, telaAtual) { telaAtual = "Fiados" }
                    NuiMenuButton("Estoque", Icons.Default.Inventory, telaAtual) { telaAtual = "Estoque" }
                    NuiMenuButton("Cardápio", Icons.Default.RestaurantMenu, telaAtual) { telaAtual = "Cardápio" }
                    NuiMenuButton("Histórico", Icons.Default.History, telaAtual) { telaAtual = "Histórico" }
                }

                // ÁREA DE CONTEÚDO DINÂMICO
                Box(Modifier.fillMaxSize().background(BackgroundCream)) {
                    when (telaAtual) {
                        "Centro de Comando" -> DashboardScreen()
                        "Estoque" -> EstoqueScreen()
                        "Novo Pedido" -> NovoPedidoScreen()
                        "Caixa" -> CaixaScreen()
                        "Fiados" -> FiadosScreen()
                        "Cardápio" -> CardapioScreen()
                        "Histórico" -> HistoricoScreen()

                        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("TELA '$telaAtual' EM CONSTRUÇÃO...", color = TextDarkBrown.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NuiMenuButton(nome: String, icone: ImageVector, telaAtual: String, onClick: () -> Unit) {
    val selecionado = nome == telaAtual
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Transições de cores baseadas no seu tema light/dark do painel lateral
    val animatedBgColor by animateColorAsState(
        targetValue = if (selecionado) SecondaryOrange else if (isHovered) OnPrimary.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150)
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (selecionado) OnPrimary else if (isHovered) OnPrimary else OnPrimary.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null, tint = animatedTextColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(nome, color = animatedTextColor, fontSize = 14.sp, fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

fun main() = application {
    val windowState = remember { WindowState(width = 1200.dp, height = 800.dp) }
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Rotisseria Admin",
        undecorated = true
    ) {
        NuiApp(windowState = windowState, onClose = ::exitApplication)
    }
}