package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseria00.models.Comanda
import com.kenji.rotisseria00.models.ItemCardapioResponse
import com.kenji.rotisseria00.models.ItemComanda
import com.kenji.rotisseria00.network.RotisseriaApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardapioSalaoScreen() { // Se for colocar no seu arquivo ComandaScreen, renomeie aqui para ComandaScreen
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)
    val corFundoCard = Color(0xFF362511)

    val coroutineScope = rememberCoroutineScope()

    // Lista do cardápio vinda do MongoDB
    var listaCardapio by remember { mutableStateOf<List<ItemCardapioResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dados do Pedido do Garçom
    var numeroMesa by remember { mutableStateOf("") }
    var nomeCliente by remember { mutableStateOf("") }
    var carrinho by remember { mutableStateOf(mapOf<ItemCardapioResponse, Int>()) }
    var enviandoPedido by remember { mutableStateOf(false) }

    // Busca o cardápio assim que a tela abre
    LaunchedEffect(Unit) {
        isLoading = true
        val cardapioCompleto = RotisseriaApi.buscarCardapio()
        // O garçom SÓ VÊ o que o Admin marcou como disponível (true)
        listaCardapio = cardapioCompleto.filter { it.disponivel }
        isLoading = false
    }

    val totalPedido = carrinho.entries.sumOf { it.key.preco * it.value }
    val cardapioAgrupado = listaCardapio.groupBy { it.categoria }

    Scaffold(
        containerColor = corFundoApp,
        bottomBar = {
            // Rodapé fixo com o Total e o Botão de Enviar (Otimizado para mobile)
            Surface(
                color = Color(0xFF362511),
                shadowElevation = 24.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth().navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total", color = corTextoClaro.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text("R$ ${"%.2f".format(totalPedido)}", color = corTextoDestaque, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (numeroMesa.isNotBlank() && carrinho.isNotEmpty()) {
                                enviandoPedido = true
                                coroutineScope.launch {
                                    // Converte o carrinho para o formato do banco
                                    val itensComanda = carrinho.map { (produto, qtd) ->
                                        ItemComanda(quantidade = qtd, nome = produto.nome, preco = produto.preco, statusCozinha = "PREPARANDO")
                                    }

                                    val novaComanda = Comanda(
                                        mesa = numeroMesa, nomeCliente = nomeCliente, itens = itensComanda,
                                        total = totalPedido, statusComanda = "EM_ABERTO"
                                    )

                                    // Envia para o Ktor (Salva no Mongo e apita na Cozinha)
                                    val sucesso = RotisseriaApi.enviarComanda(novaComanda)
                                    if (sucesso) {
                                        // Limpa a tela para o garçom atender a próxima mesa
                                        numeroMesa = ""
                                        nomeCliente = ""
                                        carrinho = emptyMap()
                                    }
                                    enviandoPedido = false
                                }
                            }
                        },
                        enabled = numeroMesa.isNotBlank() && carrinho.isNotEmpty() && !enviandoPedido,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C), disabledContainerColor = Color.Gray),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        if (enviandoPedido) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("ENVIAR PEDIDO", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {

            // --- TOPO: IDENTIFICAÇÃO DA MESA ---
            Text("NOVO PEDIDO", color = corTextoDestaque, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = numeroMesa,
                    onValueChange = { numeroMesa = it },
                    label = { Text("Mesa *", color = corTextoClaro) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque),
                    modifier = Modifier.weight(0.35f)
                )
                OutlinedTextField(
                    value = nomeCliente,
                    onValueChange = { nomeCliente = it },
                    label = { Text("Cliente (Opcional)", color = corTextoClaro) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque),
                    modifier = Modifier.weight(0.65f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = corFundoCard)
            Spacer(modifier = Modifier.height(8.dp))

            // --- LISTA DO CARDÁPIO ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = corTextoDestaque)
                }
            } else if (listaCardapio.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum item disponível no momento.", color = corTextoClaro)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    cardapioAgrupado.forEach { (categoria, produtos) ->
                        item {
                            Text(categoria, color = corTextoClaro.copy(alpha = 0.7f), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        items(produtos) { produto ->
                            val quantidade = carrinho[produto] ?: 0

                            Card(colors = CardDefaults.cardColors(containerColor = corFundoCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(produto.nome, color = corTextoClaro, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("R$ ${"%.2f".format(produto.preco)}", color = corTextoDestaque, fontSize = 14.sp)
                                    }

                                    // BOTOES DE + E - OTIMIZADOS PARA O DEDO NO CELULAR
                                    if (quantidade == 0) {
                                        IconButton(
                                            onClick = { carrinho = carrinho.toMutableMap().apply { put(produto, 1) } },
                                            modifier = Modifier.background(corTextoDestaque, RoundedCornerShape(8.dp)).size(40.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = corFundoCard)
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            IconButton(
                                                onClick = {
                                                    if (quantidade > 1) carrinho = carrinho.toMutableMap().apply { put(produto, quantidade - 1) }
                                                    else carrinho = carrinho.toMutableMap().apply { remove(produto) }
                                                },
                                                modifier = Modifier.background(corTextoClaro.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).size(36.dp)
                                            ) { Icon(Icons.Default.Remove, contentDescription = "Remover", tint = corTextoClaro) }

                                            Text(quantidade.toString(), color = corTextoClaro, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                                            IconButton(
                                                onClick = { carrinho = carrinho.toMutableMap().apply { put(produto, quantidade + 1) } },
                                                modifier = Modifier.background(corTextoDestaque, RoundedCornerShape(8.dp)).size(36.dp)
                                            ) { Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = corFundoCard) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}