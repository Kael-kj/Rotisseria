package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminScreen() {
    val corFundoBarraLateral = Color(0xFF362511)
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)

    var abaSelecionada by remember { mutableStateOf("DASHBOARD") }

    Row(modifier = Modifier.fillMaxSize().background(corFundoApp)) {

        // --- BARRA LATERAL (Agora com 5 opções) ---
        Column(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .background(corFundoBarraLateral)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp) // Diminuí um pouco o espaço para caber os 5
        ) {
            MenuLateralItem("DASHBOARD", Icons.Default.Dashboard, abaSelecionada, corTextoDestaque, corTextoClaro) { abaSelecionada = it }
            MenuLateralItem("CAIXA", Icons.Default.PointOfSale, abaSelecionada, corTextoDestaque, corTextoClaro) { abaSelecionada = it }
            MenuLateralItem("CARDÁPIO", Icons.Default.MenuBook, abaSelecionada, corTextoDestaque, corTextoClaro) { abaSelecionada = it }
            MenuLateralItem("ESTOQUE", Icons.Default.Inventory, abaSelecionada, corTextoDestaque, corTextoClaro) { abaSelecionada = it }
            MenuLateralItem("HISTÓRICO", Icons.Default.History, abaSelecionada, corTextoDestaque, corTextoClaro) { abaSelecionada = it }
            MenuLateralItem("FIADOS", Icons.Default.MoneyOff, abaSelecionada, corTextoDestaque, corTextoClaro) { abaSelecionada = it }
        }

        // --- ÁREA DE CONTEÚDO PRINCIPAL ---
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (abaSelecionada) {
                "DASHBOARD" -> NovoDashboardScreen() // A tela que vamos criar abaixo!
                "CAIXA" -> CaixaScreen()             // A tela que recebe o dinheiro (já pronta)
                "CARDÁPIO" -> CardapioScreen()
                "ESTOQUE" -> EstoqueScreen()
                "HISTÓRICO" -> HistoricoScreen()
                "FIADOS" -> FiadosScreen()
            }
        }
    }
}

@Composable
fun MenuLateralItem(titulo: String, icone: ImageVector, abaAtual: String, corDestaque: Color, corClara: Color, onClick: (String) -> Unit) {
    val isSelecionado = abaAtual == titulo
    val corFundo = if (isSelecionado) corDestaque else Color.Transparent
    val corIconeTexto = if (isSelecionado) Color(0xFF362511) else corClara

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(corFundo, RoundedCornerShape(16.dp))
            .clickable { onClick(titulo) }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icone, contentDescription = titulo, tint = corIconeTexto, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = titulo, color = corIconeTexto, fontSize = 11.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlaceholderTela(titulo: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = titulo, color = Color(0xFFEBE1CE), fontSize = 32.sp, fontFamily = FidalgaFont)
    }
}