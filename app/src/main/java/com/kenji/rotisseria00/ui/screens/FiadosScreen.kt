package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kenji.rotisseria00.models.Comanda
import com.kenji.rotisseria00.network.RotisseriaApi
import kotlinx.coroutines.launch

@Composable
fun FiadosScreen() {
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)
    val corFundoCard = Color(0xFF362511)
    val corAlerta = Color(0xFFD32F2F)

    val coroutineScope = rememberCoroutineScope()
    var listaFiados by remember { mutableStateOf<List<Comanda>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados para o Modal de Pagamento
    var clienteParaPagar by remember { mutableStateOf<String?>(null) }
    var comprasDoCliente by remember { mutableStateOf<List<Comanda>>(emptyList()) }
    var processandoPagamento by remember { mutableStateOf(false) }

    fun carregarFiados() {
        coroutineScope.launch {
            isLoading = true
            listaFiados = RotisseriaApi.buscarFiados()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { carregarFiados() }

    // Agrupa as comandas por nome do cliente
    val fiadosAgrupados = listaFiados.groupBy { it.nomeCliente ?: "Sem Nome" }

    Scaffold(containerColor = corFundoApp) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp)) {

            // CABEÇALHO DA TELA
            Text("GESTÃO DE FIADOS", color = corTextoDestaque, fontSize = 32.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = corTextoDestaque) }
            } else if (fiadosAgrupados.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ninguém está devendo no momento! 🎉", color = corTextoClaro.copy(alpha = 0.5f), fontSize = 20.sp, fontFamily = FidalgaFont)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    fiadosAgrupados.forEach { (cliente, compras) ->
                        val totalDoCliente = compras.sumOf { it.total }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = corFundoCard),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {

                                    // NOME E TOTAL
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(cliente.uppercase(), color = corTextoDestaque, fontSize = 24.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("DÍVIDA TOTAL", color = corTextoClaro.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FidalgaFont)
                                            Text("R$ ${"%.2f".format(totalDoCliente)}", color = corAlerta, fontSize = 24.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Divider(Modifier.padding(vertical = 16.dp), color = corFundoApp)

                                    // HISTÓRICO DE CONSUMO
                                    compras.forEach { compra ->
                                        Column(Modifier.padding(vertical = 8.dp)) {
                                            Text("Data: ${compra.dataFechamento}", color = corTextoClaro, fontSize = 14.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(4.dp))

                                            compra.itens.forEach { item ->
                                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                                    Text("- ${item.quantidade}x ${item.nome}", color = corTextoClaro.copy(alpha = 0.8f), fontSize = 14.sp, fontFamily = FidalgaFont)
                                                    Text("R$ ${"%.2f".format(item.preco * item.quantidade)}", color = corTextoClaro.copy(alpha = 0.6f), fontSize = 14.sp, fontFamily = FidalgaFont)
                                                }
                                            }

                                            Spacer(Modifier.height(8.dp))
                                            Text("Subtotal: R$ ${"%.2f".format(compra.total)}", color = corTextoDestaque, fontSize = 16.sp, fontFamily = FidalgaFont, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                                            Divider(Modifier.padding(top = 8.dp), color = corFundoApp.copy(alpha = 0.3f))
                                        }
                                    }

                                    // BOTÃO DE RECEBER
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            clienteParaPagar = cliente
                                            comprasDoCliente = compras
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C), contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(56.dp)
                                    ) { Text("RECEBER PAGAMENTO", fontFamily = FidalgaFont, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // MODAL DE PAGAMENTO DO FIADO
    // ==========================================
    if (clienteParaPagar != null) {
        val totalAPagar = comprasDoCliente.sumOf { it.total }

        Dialog(onDismissRequest = { if (!processandoPagamento) clienteParaPagar = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = corFundoApp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    Text("RECEBER DE ${clienteParaPagar!!.uppercase()}", color = corTextoDestaque, fontSize = 20.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Valor Total da Dívida:", color = corTextoClaro, fontSize = 16.sp, fontFamily = FidalgaFont)
                    Text("R$ ${"%.2f".format(totalAPagar)}", color = Color(0xFF388E3C), fontSize = 36.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Como o cliente está pagando?", color = corTextoClaro.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = FidalgaFont)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (processandoPagamento) {
                        CircularProgressIndicator(color = corTextoDestaque)
                    } else {
                        val metodos = listOf("PIX", "DINHEIRO", "DÉBITO", "CRÉDITO")

                        metodos.chunked(2).forEach { par ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                par.forEach { metodo ->
                                    Button(
                                        onClick = {
                                            processandoPagamento = true
                                            coroutineScope.launch {
                                                // Dá baixa em TODAS as comandas que esse cliente deve de uma vez só!
                                                comprasDoCliente.forEach { compra ->
                                                    RotisseriaApi.quitarFiado(compra.mesa, metodo)
                                                }
                                                // Terminou de dar baixa, fecha a tela e recarrega
                                                clienteParaPagar = null
                                                carregarFiados()
                                                processandoPagamento = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = corFundoCard, contentColor = corTextoDestaque),
                                        shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(56.dp)
                                    ) { Text(metodo, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold) }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { clienteParaPagar = null }) { Text("CANCELAR", color = corTextoClaro, fontFamily = FidalgaFont) }
                    }
                }
            }
        }
    }
}