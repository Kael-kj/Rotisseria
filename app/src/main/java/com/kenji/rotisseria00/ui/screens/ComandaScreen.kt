package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseria00.models.ItemCardapioResponse
import com.kenji.rotisseria00.network.RotisseriaApi
import kotlinx.coroutines.launch

// =========================================================================
// 1. O ESTADO COMPARTILHADO
// =========================================================================
object ControlePedidos {
    val comandasAbertas = mutableStateMapOf<String, MutableList<ItemPedido>>()
    val comandasNoCaixa = mutableStateListOf<ComandaFechada>()

    fun adicionarItem(mesa: String, item: ItemPedido) {
        val listaAtual = comandasAbertas[mesa]?.toMutableList() ?: mutableListOf()
        listaAtual.add(item)
        comandasAbertas[mesa] = listaAtual
    }

    fun enviarParaCozinha(mesa: String) {
        val listaAtual = comandasAbertas[mesa]?.toMutableList() ?: return
        for (i in listaAtual.indices) {
            if (listaAtual[i].status == StatusItem.AGUARDANDO) {
                listaAtual[i] = listaAtual[i].copy(status = StatusItem.NA_COZINHA)
            }
        }
        comandasAbertas[mesa] = listaAtual
    }

    fun fecharConta(mesa: String, nomeCliente: String) {
        val itens = comandasAbertas[mesa] ?: return
        if (itens.isEmpty()) return
        val valorTotal = itens.sumOf { it.preco * it.quantidade }
        val comandaFechada = ComandaFechada(mesa, nomeCliente, itens, valorTotal)
        comandasNoCaixa.add(comandaFechada)
        comandasAbertas.remove(mesa)
    }
}

data class ComandaFechada(val mesa: String, val cliente: String, val itens: List<ItemPedido>, val total: Double)
data class ItemPedido(val quantidade: Int, val nome: String, val preco: Double, val status: StatusItem)

enum class StatusItem(val texto: String, val corFundo: Color, val corTexto: Color) {
    AGUARDANDO("AGUARDANDO", Color(0xFFF8CE6A), Color(0xFF432F17)),
    NA_COZINHA("NA COZINHA", Color(0xFF388E3C), Color.White)
}

// =========================================================================
// 2. O DESIGN DA LINHA DA COMANDA
// =========================================================================
@Composable
fun ItemComandaRow(item: ItemPedido, corTextoClaro: Color, corDivisor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${item.quantidade}X", color = corTextoClaro, fontSize = 24.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.nome, color = corTextoClaro, fontSize = 20.sp, fontFamily = FidalgaFont)
                Text("R$ ${"%.2f".format(item.preco)}", color = corTextoClaro.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = FidalgaFont)
            }
            Box(
                modifier = Modifier.background(item.status.corFundo, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(item.status.texto, color = item.status.corTexto, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FidalgaFont)
            }
        }
        Divider(color = corDivisor, thickness = 1.dp)
    }
}

// =========================================================================
// 3. A TELA NOVA (COM CATEGORIAS E PRATOS) E SISTEMA DE ESTOQUE
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComandaScreen(numeroMesa: String, onVoltar: () -> Unit) {
    val corFundoHeader = Color(0xFF5A4A32)
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)
    val corFundoCard = Color(0xFF362511)
    val corDivisor = Color(0xFF5A4A32)

    val coroutineScope = rememberCoroutineScope()
    var nomeCliente by remember { mutableStateOf("") }

    var mostrandoCardapio by remember { mutableStateOf(false) }
    var categoriaSelecionada by remember { mutableStateOf<String?>(null) }

    var cardapioReal by remember { mutableStateOf<List<ItemCardapioResponse>>(emptyList()) }
    var isLoadingCardapio by remember { mutableStateOf(false) }

    var carrinho by remember { mutableStateOf(mapOf<ItemCardapioResponse, Int>()) }
    val itensDestaMesa = ControlePedidos.comandasAbertas[numeroMesa] ?: emptyList()

    LaunchedEffect(Unit) {
        isLoadingCardapio = true
        cardapioReal = RotisseriaApi.buscarCardapio().filter { it.disponivel }
        isLoadingCardapio = false
    }

    if (mostrandoCardapio) {
        val totalCarrinho = carrinho.entries.sumOf { it.key.preco * it.value }

        Scaffold(
            containerColor = corFundoApp,
            bottomBar = {
                if (carrinho.isNotEmpty()) {
                    Surface(color = corFundoCard, shadowElevation = 24.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                        Row(
                            modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Novos Itens", color = corTextoClaro.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = FidalgaFont)
                                Text("R$ ${"%.2f".format(totalCarrinho)}", color = corTextoDestaque, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FidalgaFont)
                            }
                            Button(
                                onClick = {
                                    carrinho.forEach { (produto, qtd) ->
                                        val novoItem = ItemPedido(qtd, produto.nome, produto.preco, StatusItem.AGUARDANDO)
                                        ControlePedidos.adicionarItem(numeroMesa, novoItem)
                                    }
                                    carrinho = emptyMap()
                                    categoriaSelecionada = null
                                    mostrandoCardapio = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = corTextoDestaque, contentColor = corFundoCard),
                                shape = RoundedCornerShape(12.dp), modifier = Modifier.height(56.dp)
                            ) { Text("ADICIONAR À MESA", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FidalgaFont) }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {

                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    IconButton(
                        onClick = {
                            if (categoriaSelecionada != null) categoriaSelecionada = null
                            else mostrandoCardapio = false
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = corTextoClaro)
                    }

                    Text(
                        text = if (categoriaSelecionada == null) "CATEGORIAS" else categoriaSelecionada!!.uppercase(),
                        color = corTextoDestaque,
                        fontSize = 28.sp,
                        fontFamily = FidalgaFont,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (isLoadingCardapio) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = corTextoDestaque) }
                } else {
                    if (categoriaSelecionada == null) {
                        val categoriasExistentes = cardapioReal.map { it.categoria }.distinct()

                        if (categoriasExistentes.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Nenhum item disponível no cardápio.", color = corTextoClaro, fontFamily = FidalgaFont)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                items(categoriasExistentes) { categoria ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = corFundoCard),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.height(120.dp).clickable { categoriaSelecionada = categoria }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = corTextoDestaque, modifier = Modifier.size(32.dp))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(categoria, color = corTextoClaro, fontSize = 18.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val produtosDaCategoria = cardapioReal.filter { it.categoria == categoriaSelecionada }

                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                            // --- REGRAS DO PRATO DO DIA EMBUTIDAS AQUI ---
                            items(produtosDaCategoria) { produto ->
                                val quantidade = carrinho[produto] ?: 0

                                val temLimite = produto.estoqueAtual != null
                                val estoqueAtual = produto.estoqueAtual ?: 0
                                val esgotado = temLimite && estoqueAtual <= 0
                                val atingiuLimiteDoCarrinho = temLimite && quantidade >= estoqueAtual

                                Card(colors = CardDefaults.cardColors(containerColor = corFundoCard), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(produto.nome, color = corTextoClaro, fontSize = 18.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("R$ ${"%.2f".format(produto.preco)}", color = corTextoDestaque, fontSize = 16.sp, fontFamily = FidalgaFont)

                                                // MOSTRA "RESTAM X" OU "ESGOTADO"
                                                if (temLimite) {
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Box(modifier = Modifier.background(if (esgotado) Color.Red.copy(alpha = 0.2f) else corTextoDestaque.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                        Text(
                                                            text = if (esgotado) "ESGOTADO" else "Restam $estoqueAtual",
                                                            color = if (esgotado) Color(0xFFFF6B6B) else corTextoDestaque,
                                                            fontSize = 12.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // BLOQUEIA O BOTÃO DE ADICIONAR SE ACABOU
                                        if (quantidade == 0) {
                                            IconButton(
                                                onClick = { if (!esgotado) carrinho = carrinho.toMutableMap().apply { put(produto, 1) } },
                                                modifier = Modifier.background(if (esgotado) corTextoClaro.copy(alpha = 0.2f) else corTextoDestaque, RoundedCornerShape(8.dp)).size(40.dp)
                                            ) { Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = corFundoCard) }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        if (quantidade > 1) carrinho = carrinho.toMutableMap().apply { put(produto, quantidade - 1) }
                                                        else carrinho = carrinho.toMutableMap().apply { remove(produto) }
                                                    },
                                                    modifier = Modifier.background(corTextoClaro.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).size(36.dp)
                                                ) { Icon(Icons.Default.Remove, contentDescription = "Remover", tint = corTextoClaro) }

                                                Text(quantidade.toString(), color = corTextoClaro, fontSize = 20.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)

                                                IconButton(
                                                    onClick = { if (!atingiuLimiteDoCarrinho) carrinho = carrinho.toMutableMap().apply { put(produto, quantidade + 1) } },
                                                    modifier = Modifier.background(if (atingiuLimiteDoCarrinho) corTextoClaro.copy(alpha = 0.2f) else corTextoDestaque, RoundedCornerShape(8.dp)).size(36.dp)
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
    else {
        Scaffold(
            containerColor = corFundoApp,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        categoriaSelecionada = null
                        mostrandoCardapio = true
                    },
                    containerColor = corTextoDestaque,
                    contentColor = corFundoApp,
                    shape = RoundedCornerShape(16.dp)
                ) { Icon(Icons.Default.Add, contentDescription = "Adicionar Item", modifier = Modifier.size(32.dp)) }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

                Box(
                    modifier = Modifier.fillMaxWidth().background(corFundoHeader, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 16.dp)
                ) {
                    IconButton(onClick = onVoltar, modifier = Modifier.align(Alignment.TopStart)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = corTextoClaro)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Text(
                            text = "MESA $numeroMesa",
                            color = corTextoDestaque,
                            fontSize = 32.sp,
                            fontFamily = FidalgaFont,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = nomeCliente, onValueChange = { nomeCliente = it }, placeholder = { Text("Nome do Cliente", color = corTextoClaro.copy(alpha = 0.5f), fontFamily = FidalgaFont) },
                            modifier = Modifier.fillMaxWidth(0.9f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FidalgaFont, fontSize = 18.sp)
                        )
                    }
                }

                if (itensDestaMesa.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Comanda Vazia. Clique no + para abrir o cardápio.", color = corTextoClaro.copy(alpha = 0.5f), fontFamily = FidalgaFont)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
                        items(itensDestaMesa) { item -> ItemComandaRow(item, corTextoClaro, corDivisor) }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            if (itensDestaMesa.isNotEmpty()) {
                                coroutineScope.launch {
                                    val itensParaEnvio = itensDestaMesa.map { com.kenji.rotisseria00.models.ItemComanda(it.quantidade, it.nome, it.preco, "AGUARDANDO") }
                                    val valorTotal = itensDestaMesa.sumOf { it.preco * it.quantidade }
                                    val novaComanda = com.kenji.rotisseria00.models.Comanda(mesa = numeroMesa, nomeCliente = nomeCliente, itens = itensParaEnvio, total = valorTotal, statusComanda = "EM_ABERTO")

                                    val sucesso = RotisseriaApi.enviarComanda(novaComanda)
                                    if (sucesso) ControlePedidos.enviarParaCozinha(numeroMesa)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = corTextoDestaque, contentColor = corFundoApp)
                    ) { Text("ENVIAR PARA COZINHA", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FidalgaFont) }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val sucesso = RotisseriaApi.fecharConta(numeroMesa)
                                if (sucesso) {
                                    ControlePedidos.comandasAbertas.remove(numeroMesa)
                                    onVoltar()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = corTextoClaro, contentColor = corFundoApp)
                    ) { Text("FECHAR CONTA / IR PRO CAIXA", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FidalgaFont) }
                }
            }
        }
    }
}