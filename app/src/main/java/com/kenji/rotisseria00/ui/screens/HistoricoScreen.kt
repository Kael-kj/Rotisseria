package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseria00.models.Comanda
import com.kenji.rotisseria00.network.RotisseriaApi
import kotlinx.coroutines.launch


@Composable
fun HistoricoScreen() {
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)
    val corFundoCard = Color(0xFF362511)
    val corDivisor = Color(0xFF5A4A32)

    val coroutineScope = rememberCoroutineScope()
    var listaHistorico by remember { mutableStateOf<List<Comanda>>(emptyList()) }
    var comandaSelecionada by remember { mutableStateOf<Comanda?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Busca o arquivo morto assim que a tela abre
    LaunchedEffect(Unit) {
        isLoading = true
        // O Ktor vai trazer a lista da mais antiga para a mais nova, então nós revertemos
        // para a mais recente aparecer primeiro no topo da lista
        listaHistorico = RotisseriaApi.buscarHistorico().reversed()
        if (listaHistorico.isNotEmpty()) {
            comandaSelecionada = listaHistorico.first()
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(corFundoApp)
            .padding(32.dp)
    ) {
        Text(
            text = "HISTÓRICO DE PEDIDOS",
            color = corTextoDestaque,
            fontSize = 32.sp,
            fontFamily = FidalgaFont,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = corTextoDestaque)
            }
        } else if (listaHistorico.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma venda concluída ainda.", color = corTextoClaro.copy(alpha = 0.5f), fontSize = 20.sp, fontFamily = FidalgaFont)
            }
        } else {
            // --- LAYOUT MASTER-DETAIL ---
            Row(modifier = Modifier.fillMaxSize()) {

                // COLUNA ESQUERDA: LISTA DE COMANDAS PAGAS
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pedidos Finalizados", color = corTextoClaro, fontSize = 20.sp, fontFamily = FidalgaFont)
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(listaHistorico) { comanda ->
                            val isSelecionado = comandaSelecionada?.mesa == comanda.mesa && comandaSelecionada?.dataFechamento == comanda.dataFechamento

                            CardItemHistorico(
                                comanda = comanda,
                                isSelecionado = isSelecionado,
                                corDestaque = corTextoDestaque,
                                corFundoCard = corFundoCard,
                                corClara = corTextoClaro,
                                onClick = { comandaSelecionada = comanda }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(corDivisor))
                Spacer(modifier = Modifier.width(32.dp))

                // COLUNA DIREITA: DETALHES DA COMANDA (O RECIBO)
                Column(modifier = Modifier.weight(1.2f)) {
                    comandaSelecionada?.let { comanda ->
                        CardDetalhesHistorico(
                            comanda = comanda,
                            corFundoCard = corFundoCard,
                            corDestaque = corTextoDestaque,
                            corClara = corTextoClaro
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CardItemHistorico(
    comanda: Comanda,
    isSelecionado: Boolean,
    corDestaque: Color,
    corFundoCard: Color,
    corClara: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelecionado) corDestaque else corFundoCard
    val textColor = if (isSelecionado) Color(0xFF362511) else corClara

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Mesa ${comanda.mesa}",
                    color = textColor, fontSize = 20.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold
                )
                Text(
                    text = "R$ ${"%.2f".format(comanda.total)}",
                    color = if (isSelecionado) Color(0xFF362511) else Color(0xFF388E3C), // Fica verde se não estiver selecionado
                    fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = comanda.dataFechamento ?: "Data não registrada",
                    color = textColor.copy(alpha = 0.8f), fontSize = 14.sp, fontFamily = FidalgaFont
                )
                Text(
                    text = comanda.statusComanda,
                    color = textColor.copy(alpha = 0.8f), fontSize = 14.sp, fontFamily = FidalgaFont
                )
            }
        }
    }
}

@Composable
fun CardDetalhesHistorico(comanda: Comanda, corFundoCard: Color, corDestaque: Color, corClara: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = corFundoCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text("Detalhes do Pedido", color = corClara, fontSize = 24.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
            Text("Mesa ${comanda.mesa} - Cliente: ${if(comanda.nomeCliente.isBlank()) "Balcão" else comanda.nomeCliente}", color = corClara.copy(alpha = 0.7f), fontSize = 18.sp, fontFamily = FidalgaFont)

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Lista de Itens Consumidos
            Text("ITENS CONSUMIDOS:", color = corClara.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = FidalgaFont)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) { // Limita a altura para não empurrar os botões pra fora da tela
                items(comanda.itens) { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${item.quantidade}x ${item.nome}", color = corClara, fontSize = 16.sp, fontFamily = FidalgaFont)
                        Text("R$ ${"%.2f".format(item.preco * item.quantidade)}", color = corClara, fontSize = 16.sp, fontFamily = FidalgaFont)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))

            // Resumo Financeiro
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("TOTAL PAGO:", color = corClara.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = FidalgaFont)
                    Text("R$ ${"%.2f".format(comanda.total)}", color = Color(0xFF388E3C), fontSize = 28.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("MÉTODO DE PAGAMENTO:", color = corClara.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = FidalgaFont)
                    Text(comanda.metodoPagamento ?: "Não informado", color = corDestaque, fontSize = 24.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botão de Reimprimir
            Button(
                onClick = { /* TODO: Lógica de integração com impressora térmica Bluetooth */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = corDestaque, contentColor = Color(0xFF362511))
            ) {
                Icon(Icons.Default.Print, contentDescription = "Imprimir", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("REIMPRIMIR COMPROVANTE", fontSize = 18.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
            }
        }
    }
}