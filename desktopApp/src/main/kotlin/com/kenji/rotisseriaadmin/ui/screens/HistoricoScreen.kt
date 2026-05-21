package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Print
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseriaadmin.data.ComandaResponse
import com.kenji.rotisseriaadmin.data.ItemComanda
import com.kenji.rotisseriaadmin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HistoricoScreen() {
    val coroutineScope = rememberCoroutineScope()

    // Estados dos Dados
    var historicoComandas by remember { mutableStateOf<List<ComandaResponse>>(emptyList()) }
    var comandaSelecionada by remember { mutableStateOf<ComandaResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados dos Filtros
    val filtrosTempo = listOf("HOJE", "ESTA SEMANA", "ESTE MÊS", "TUDO")
    var filtroTempoSelecionado by remember { mutableStateOf("TUDO") }

    var filtroPrato by remember { mutableStateOf("Todos") }
    var filtroCategoria by remember { mutableStateOf("Todas") }
    var filtroParceria by remember { mutableStateOf("Todas") }

    // Simulação de carregamento (Substitua por RotisseriaApi.buscarHistorico())
    LaunchedEffect(Unit) {
        isLoading = true
        // Mock de dados baseado na sua imagem
        historicoComandas = listOf(
            ComandaResponse("MESA 10", "BALCÃO", listOf(
                ItemComanda(1, "Batata Frita", 20.0, "PRONTO", "Acompanhamentos", "Nenhuma")
            ), 20.0, "FECHADA", "Cartão Crédito", "06/04/2026 às 15:54"),
            ComandaResponse("MESA 02", "MARCOS", listOf(), 45.0, "FECHADA", "Pix", "Data não registrada"),
            ComandaResponse("MESA 01", "JOÃO", listOf(), 45.0, "FECHADA", "Dinheiro", "14/05/2026 às 15:36"),
            ComandaResponse("MESA 05", "MARIA", listOf(), 12.0, "FECHADA", "Cartão Débito", "Data não registrada"),
            ComandaResponse("MESA 06", "CLIENTE", listOf(), 60.0, "FECHADA", "Pix", "Data não registrada"),
            ComandaResponse("MESA 03", "CLIENTE", listOf(), 57.0, "FECHADA", "Dinheiro", "Data não registrada"),
            ComandaResponse("VIAGEM", "BALCÃO", listOf(
                ItemComanda(1, "Batata Frita", 20.0, "PRONTO", "Acompanhamentos", "Nenhuma"),
                ItemComanda(1, "Arroz", 5.0, "PRONTO", "Acompanhamentos", "Nenhuma"),
                ItemComanda(2, "Lasanha", 55.0, "PRONTO", "Prato principal", "Nenhuma"),
                ItemComanda(2, "Carne desfiada", 35.0, "PRONTO", "Prato principal", "Nenhuma"),
                ItemComanda(1, "Joelho de Porco Assado", 125.0, "PRONTO", "Prato principal", "Nenhuma")
            ), 330.0, "FECHADA", "Cartão Crédito", "06/04/2026 às 16:07")
        )
        isLoading = false
    }

    val totalVendido = historicoComandas.sumOf { it.total }

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (isLoading) {
            CircularProgressIndicator(color = PrimaryBrown, modifier = Modifier.align(Alignment.Center))
        } else {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                // ==========================================
                // COLUNA DA ESQUERDA: FILTROS E LISTA
                // ==========================================
                Column(modifier = Modifier.weight(1.3f)) {
                    Text("HISTÓRICO DE VENDAS", color = TextDarkBrown, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    // LINHA 1: Filtros de Tempo (Chips)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        filtrosTempo.forEach { tempo ->
                            val isSelecionado = tempo == filtroTempoSelecionado
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelecionado) SecondaryOrange else SurfaceWhite)
                                    .clickable { filtroTempoSelecionado = tempo }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = tempo,
                                    color = if (isSelecionado) SurfaceWhite else TextDarkBrown,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // LINHA 2: Filtros Dropdown (Prato, Categoria, Parceria)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FiltroDropdown("Prato", filtroPrato, modifier = Modifier.weight(1f))
                        FiltroDropdown("Categoria", filtroCategoria, modifier = Modifier.weight(1f))
                        FiltroDropdown("Parceria", filtroParceria, modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SecondaryOrange)
                                .clickable { /* Aplicar filtros extras se houver */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtrar", tint = SurfaceWhite)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // LINHA 3: Resumo (Contagem e Total)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Pedidos Finalizados (${historicoComandas.size})", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("R$ ${"%.2f".format(totalVendido)}", color = SecondaryOrange, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // LISTA DE VENDAS FINALIZADAS
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(historicoComandas) { comanda ->
                            val isSelecionada = comanda == comandaSelecionada
                            Card(
                                backgroundColor = if (isSelecionada) BackgroundCream else SurfaceWhite,
                                elevation = if (isSelecionada) 4.dp else 1.dp,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().clickable { comandaSelecionada = comanda }
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(comanda.mesa.uppercase(), color = TextDarkBrown, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(comanda.dataFechamento ?: "Data não registrada", color = TextDarkBrown.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("R$ ${"%.2f".format(comanda.total)}", color = CorVerde, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("PAGO", color = TextDarkBrown.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // COLUNA DA DIREITA: DETALHES DO PEDIDO
                // ==========================================
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    if (comandaSelecionada == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Selecione um pedido ao lado para ver os detalhes", color = TextDarkBrown.copy(alpha = 0.3f), fontSize = 18.sp)
                        }
                    } else {
                        val comanda = comandaSelecionada!!

                        // PARTE SUPERIOR: Itens Consumidos
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Detalhes do Pedido", color = TextDarkBrown, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${comanda.mesa} - Cliente: ${comanda.nomeCliente}", color = TextDarkBrown.copy(alpha = 0.7f), fontSize = 16.sp)

                            Divider(color = SurfaceWhite, thickness = 2.dp, modifier = Modifier.padding(vertical = 24.dp))

                            Text("ITENS CONSUMIDOS:", color = TextDarkBrown, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(comanda.itens) { item ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${item.quantidade}x ${item.nome}", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        Text("R$ ${"%.2f".format(item.preco * item.quantidade)}", color = TextDarkBrown.copy(alpha = 0.7f), fontSize = 16.sp)
                                    }
                                }
                            }
                        }

                        // PARTE INFERIOR: Totais e Reimpressão
                        Column {
                            Divider(color = SurfaceWhite, thickness = 2.dp, modifier = Modifier.padding(vertical = 16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Column {
                                    Text("TOTAL PAGO:", color = TextDarkBrown.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("R$ ${"%.2f".format(comanda.total)}", color = CorVerde, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("MÉTODO DE PAGAMENTO:", color = TextDarkBrown.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(comanda.metodoPagamento ?: "Não informado", color = TextDarkBrown, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // BOTÃO REIMPRIMIR
                            Button(
                                onClick = { /* TODO: Chamar lógica de impressão */ },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = SecondaryOrange, contentColor = SurfaceWhite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Imprimir", modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("REIMPRIMIR COMPROVANTE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Componente auxiliar para os Dropdowns menores de filtro
@Composable
fun FiltroDropdown(label: String, value: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.clickable { expanded = true }) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryBrown,
                unfocusedBorderColor = SurfaceWhite,
                textColor = TextDarkBrown
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        // Adicione os itens do Dropdown aqui futuramente se precisar
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(onClick = { expanded = false }) { Text("Todos") }
        }
    }
}