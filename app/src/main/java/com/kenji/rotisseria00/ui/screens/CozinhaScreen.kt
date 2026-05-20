package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
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
import com.kenji.rotisseria00.utils.SoundPlayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CozinhaScreen() {
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corFundoCard = Color(0xFF362511)
    val corTextoClaro = Color(0xFFEBE1CE)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pedidos by remember { mutableStateOf<List<Comanda>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Lista de IDs (mesas) que acabamos de finalizar, para evitar que o "poller" traga de volta
    // antes do servidor processar a mudança de status.
    val mesasFinalizadas = remember { mutableStateSetOf<String>() }

    // Atualização em tempo real (Loop a cada 5s para novos pedidos)
    LaunchedEffect(Unit) {
        var pedidosAnteriores = emptyList<Comanda>()
        
        while (true) {
            try {
                val novosPedidos = RotisseriaApi.buscarPedidosCozinha()
                val pedidosFiltrados = novosPedidos.filter { it.mesa !in mesasFinalizadas }
                
                // Se o número de pedidos aumentou, toca o som
                if (pedidosFiltrados.size > pedidosAnteriores.size) {
                    SoundPlayer.playNotificationSound(context)
                }
                
                pedidos = pedidosFiltrados
                pedidosAnteriores = pedidosFiltrados
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isLoading = false
            delay(5000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(corFundoApp).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Monitor da Cozinha",
                color = corTextoDestaque,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (isLoading && pedidos.isEmpty()) {
                CircularProgressIndicator(color = corTextoDestaque, modifier = Modifier.size(24.dp))
            } else {
                Text("${pedidos.size} Pedidos", color = corTextoClaro, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pedidos.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum pedido pendente", color = corTextoClaro.copy(alpha = 0.5f), fontSize = 20.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pedidos) { pedido ->
                    CardPedidoCozinha(
                        pedido = pedido,
                        corFundoCard = corFundoCard,
                        corTextoClaro = corTextoClaro,
                        corTextoDestaque = corTextoDestaque,
                        onFinalizar = {
                            val mesa = pedido.mesa
                            
                            // 1. Marca como finalizada localmente (Lista de Bloqueio)
                            mesasFinalizadas.add(mesa)
                            
                            // 2. Remove da lista visível imediatamente
                            pedidos = pedidos.filter { it.mesa != mesa }
                            
                            // 3. Avisa o servidor
                            coroutineScope.launch {
                                val sucesso = RotisseriaApi.concluirPedidoCozinha(mesa)
                                if (!sucesso) {
                                    // Se falhou, removemos do bloqueio para que o pedido volte a aparecer na próxima busca
                                    mesasFinalizadas.remove(mesa)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CardPedidoCozinha(
    pedido: Comanda,
    corFundoCard: Color,
    corTextoClaro: Color,
    corTextoDestaque: Color,
    onFinalizar: () -> Unit
) {
    // Estado para o tempo decorrido
    var tempoDecorridoStr by remember { mutableStateOf("00:00") }
    var corTempo by remember { mutableStateOf(Color(0xFF4CAF50)) } // Verde inicial

    // Efeito para atualizar o contador a cada segundo
    LaunchedEffect(pedido.dataEnvioCozinha) {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val dataEnvio = try {
            LocalDateTime.parse(pedido.dataEnvioCozinha, formatter)
        } catch (_: Exception) {
            LocalDateTime.now()
        }

        while (true) {
            val agora = LocalDateTime.now()
            val duracao = Duration.between(dataEnvio, agora)
            val minutos = duracao.toMinutes()
            val segundos = duracao.minusMinutes(minutos).seconds

            tempoDecorridoStr = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)

            // Lógica de cores baseada nos minutos
            corTempo = when {
                minutos < 5 -> Color(0xFF4CAF50) // Verde
                minutos < 7 -> Color(0xFFFBC02D) // Amarelo (Amber 700)
                else -> Color(0xFFD32F2F)        // Vermelho
            }

            delay(1000)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = corFundoCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MESA ${pedido.mesa}",
                    color = corTextoDestaque,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                // Contador de Tempo
                Surface(
                    color = corTempo,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tempoDecorridoStr,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Text(
                text = "Cliente: ${pedido.nomeCliente.ifEmpty { "Não informado" }}",
                color = corTextoClaro.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = corTextoClaro.copy(alpha = 0.1f)
            )

            // Itens do Pedido
            pedido.itens.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantidade}x ${item.nome}",
                        color = corTextoClaro,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    // Chamar API para finalizar
                    onFinalizar()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("PRONTO", fontWeight = FontWeight.Bold)
            }
        }
    }
}