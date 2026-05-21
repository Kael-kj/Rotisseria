package com.kenji.rotisseriaadmin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kenji.rotisseriaadmin.ui.theme.CorDestaque
import com.kenji.rotisseriaadmin.ui.theme.CorFundoLateral

@Composable
fun MenuSidebar(telaAtual: String, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(CorFundoLateral)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Rotisseria",
            color = CorDestaque,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(32.dp))

        val menuItems = listOf(
            "Centro de Comando", "Novo Pedido", "Caixa",
            "Fiados", "Estoque", "Cardápio", "Histórico"
        )

        menuItems.forEach { item ->
            MenuButton(nome = item, telaAtual = telaAtual) { onNavigate(item) }
        }
    }
}

@Composable
private fun MenuButton(nome: String, telaAtual: String, onClick: () -> Unit) {
    val selecionado = nome == telaAtual
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (selecionado) CorDestaque else Color.Transparent,
            contentColor = if (selecionado) Color.Black else Color.LightGray
        ),
        elevation = if (selecionado) ButtonDefaults.elevation() else ButtonDefaults.elevation(0.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text(nome, fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal)
        }
    }
}