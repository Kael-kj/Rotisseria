package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseriaadmin.data.ComandaResponse
import com.kenji.rotisseriaadmin.data.RotisseriaApi
import com.kenji.rotisseriaadmin.data.ItemCardapioResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

// ==========================================
// PALETA DE CORES
// ==========================================
val CorFundoApp = Color(0xFFEBE1CE) // Fundo claro para desktop
val CorFundoCard = Color(0xFFFFFFFF)
val CorTextoEscuro = Color(0xFF432F17) // Marrom escuro principal
val CorDestaque = Color(0xFFF8CE6A)    // Amarelo
val CorAlerta = Color(0xFFD32F2F)
val CorVerde = Color(0xFF388E3C)

@Composable
fun NovoPedidoScreen() {
    var mesaSelecionada by remember { mutableStateOf<String?>(null) }
    var comandasAbertas by remember { mutableStateOf<List<ComandaResponse>>(emptyList()) }
    var mesaBusca by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Sincroniza as mesas abertas com o servidor a cada 3 segundos
    LaunchedEffect(Unit) {
        while (true) {
            try {
                comandasAbertas = RotisseriaApi.buscarContasAbertas()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(3000)
        }
    }

    if (mesaSelecionada == null) {
        // ==========================================
        // TELA 1: GRID DE MESAS
        // ==========================================
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
            Text(
                text = "Novo Pedido / Mesas",
                color = CorTextoEscuro,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // BARRA PARA ADICIONAR MESA CUSTOMIZADA (Ex: Mesa 50, Mesa Jardim)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                OutlinedTextField(
                    value = mesaBusca,
                    onValueChange = { mesaBusca = it },
                    label = { Text("Buscar ou Abrir Nova Mesa (Ex: 30, Jardim...)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = CorTextoEscuro,
                        cursorColor = CorTextoEscuro,
                        textColor = CorTextoEscuro
                    )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        if (mesaBusca.isNotBlank()) {
                            // Se o usuário digitou só um número, adiciona "Mesa " na frente
                            val nomeFinal = if (mesaBusca.all { it.isDigit() }) "Mesa $mesaBusca" else mesaBusca
                            mesaSelecionada = nomeFinal.uppercase()
                            mesaBusca = ""
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = CorTextoEscuro, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ABRIR MESA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Junta as 20 mesas padrão com qualquer outra mesa customizada que esteja aberta no banco
            val mesasPadrao = (1..20).map { "MESA $it" } + listOf("BALCÃO", "VIAGEM")
            val mesasDoBanco = comandasAbertas.map { it.mesa.uppercase() }
            val listaMesas = (mesasPadrao + mesasDoBanco).distinct() // .distinct() evita duplicatas

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(listaMesas) { nomeMesa ->
                    val comandaAtiva = comandasAbertas.find { it.mesa.equals(nomeMesa, ignoreCase = true) }
                    val ocupada = comandaAtiva != null

                    MesaCardDesktop(
                        numero = nomeMesa.replace("MESA ", "", ignoreCase = true),
                        nomeCompleto = nomeMesa,
                        ocupada = ocupada,
                        nomeCliente = comandaAtiva?.nomeCliente ?: "",
                        onClick = { mesaSelecionada = nomeMesa }
                    )
                }
            }
        }
    } else {
        // ==========================================
        // TELA 2: DETALHES DO PEDIDO
        // ==========================================
        DetalhePedidoDesktop(
            numeroMesa = mesaSelecionada!!,
            comandaAtiva = comandasAbertas.find { it.mesa.equals(mesaSelecionada, ignoreCase = true) },
            onVoltar = { mesaSelecionada = null }
        )
    }
}

// ==========================================
// COMPONENTE: CARD DA MESA
// ==========================================
@Composable
fun MesaCardDesktop(numero: String, nomeCompleto: String, ocupada: Boolean, nomeCliente: String, onClick: () -> Unit) {
    // Regra de cores aplicada: Livre = Fundo Marrom e Letras Brancas / Ocupada = Fundo Amarelo e Letras Marrons
    val corFundo = if (ocupada) CorDestaque else CorTextoEscuro
    val corConteudo = if (ocupada) CorTextoEscuro else Color.White

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(corFundo, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = if (nomeCompleto.contains("MESA", ignoreCase = true)) "MESA" else nomeCompleto.uppercase(),
                color = corConteudo.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            if (nomeCompleto.contains("MESA", ignoreCase = true)) {
                Text(text = numero, color = corConteudo, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Icon(Icons.Default.TakeoutDining, contentDescription = null, tint = corConteudo, modifier = Modifier.size(40.dp))
            }

            if (ocupada) {
                if (nomeCliente.isNotEmpty()) {
                    Text(
                        text = nomeCliente,
                        color = corConteudo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(text = "Ocupada", color = corConteudo, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Schedule, contentDescription = "Ocupada", tint = corConteudo, modifier = Modifier.size(12.dp))
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ==========================================
// TELA DIVIDIDA: CARDÁPIO (ESQUERDA) E COMANDA (DIREITA)
// ==========================================
@Composable
fun DetalhePedidoDesktop(numeroMesa: String, comandaAtiva: ComandaResponse?, onVoltar: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    // Estados do Cardápio e Carrinho
    var cardapioReal by remember { mutableStateOf<List<ItemCardapioResponse>>(emptyList()) }
    var categoriaSelecionada by remember { mutableStateOf<String?>(null) }
    var carrinho by remember { mutableStateOf(mapOf<ItemCardapioResponse, Int>()) }
    var nomeCliente by remember { mutableStateOf(comandaAtiva?.nomeCliente ?: "") }

    // Alertas e Controles de Fluxo
    var erroConexao by remember { mutableStateOf<String?>(null) }
    var processando by remember { mutableStateOf(false) }
    var processandoFechamento by remember { mutableStateOf(false) }
    var itemSendoCancelado by remember { mutableStateOf<String?>(null) } // Guarda o nome do item a ser excluído

    LaunchedEffect(Unit) {
        while (true) {
            try {
                cardapioReal = RotisseriaApi.buscarCardapio().filter { it.disponivel }
            } catch (e: Exception) { e.printStackTrace() }
            delay(3000)
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(CorFundoApp)) {

        // ---------------------------------------------------------
        // PAINEL ESQUERDO: MENU / CATEGORIAS
        // ---------------------------------------------------------
        Column(modifier = Modifier.weight(1.2f).fillMaxHeight().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVoltar) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = CorTextoEscuro)
                }
                Text("Cardápio", color = CorTextoEscuro, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (categoriaSelecionada == null) {
                val categorias = cardapioReal.map { it.categoria }.distinct()
                LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(categorias) { categoria ->
                        Card(
                            backgroundColor = CorFundoCard,
                            shape = RoundedCornerShape(12.dp),
                            elevation = 2.dp,
                            modifier = Modifier.height(100.dp).clickable { categoriaSelecionada = categoria }
                        ) {
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = CorDestaque, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(categoria, color = CorTextoEscuro, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { categoriaSelecionada = null }) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar Categorias", tint = CorTextoEscuro) }
                    Text(categoriaSelecionada!!.uppercase(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CorTextoEscuro)
                }
                Spacer(modifier = Modifier.height(16.dp))

                val produtos = cardapioReal.filter { it.categoria == categoriaSelecionada }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(produtos) { produto ->
                        val quantidade = carrinho[produto] ?: 0
                        val estoqueAtual = produto.estoqueAtual ?: 0
                        val temLimite = produto.estoqueAtual != null
                        val esgotado = temLimite && estoqueAtual <= 0
                        val limiteAtingido = temLimite && quantidade >= estoqueAtual

                        Card(backgroundColor = CorFundoCard, shape = RoundedCornerShape(12.dp), elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(produto.nome, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CorTextoEscuro)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("R$ ${"%.2f".format(produto.preco)}", color = CorTextoEscuro, fontWeight = FontWeight.Bold)
                                        if (temLimite) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (esgotado) "ESGOTADO" else "Restam $estoqueAtual", color = if (esgotado) CorAlerta else CorVerde, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (quantidade == 0) {
                                    IconButton(
                                        onClick = { if (!esgotado) carrinho = carrinho.toMutableMap().apply { put(produto, 1) } },
                                        modifier = Modifier.background(if (esgotado) Color.LightGray else CorTextoEscuro, RoundedCornerShape(8.dp)).size(40.dp)
                                    ) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White) }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (quantidade > 1) carrinho = carrinho.toMutableMap().apply { put(produto, quantidade - 1) }
                                                else carrinho = carrinho.toMutableMap().apply { remove(produto) }
                                            },
                                            modifier = Modifier.background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).size(36.dp)
                                        ) { Icon(Icons.Default.Remove, contentDescription = "Remover", tint = CorTextoEscuro) }

                                        Text(quantidade.toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CorTextoEscuro)

                                        IconButton(
                                            onClick = { if (!limiteAtingido) carrinho = carrinho.toMutableMap().apply { put(produto, quantidade + 1) } },
                                            modifier = Modifier.background(if (limiteAtingido) Color.LightGray else CorTextoEscuro, RoundedCornerShape(8.dp)).size(36.dp)
                                        ) { Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // PAINEL DIREITO: RESUMO DA COMANDA E CARRINHO
        // ---------------------------------------------------------
        Column(modifier = Modifier.weight(0.8f).fillMaxHeight().background(CorFundoCard).padding(24.dp)) {
            Text(numeroMesa.uppercase(), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = CorTextoEscuro)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nomeCliente,
                onValueChange = { nomeCliente = it },
                label = { Text("Nome do Cliente") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = CorTextoEscuro, textColor = CorTextoEscuro, cursorColor = CorTextoEscuro)
            )
            Spacer(modifier = Modifier.height(24.dp))

            val itensEnviados = comandaAtiva?.itens ?: emptyList()

            LazyColumn(modifier = Modifier.weight(1f)) {
                // ITENS JÁ NO SERVIDOR
                if (itensEnviados.isNotEmpty()) {
                    item { Text("Já na Comanda", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp)) }
                    items(itensEnviados) { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.quantidade}X", fontWeight = FontWeight.Bold, color = CorTextoEscuro, modifier = Modifier.width(40.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.nome, fontWeight = FontWeight.Medium, color = CorTextoEscuro)
                                Text("R$ ${"%.2f".format(item.preco)}", color = Color.Gray, fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.background(if(item.statusCozinha == "PRONTO") CorVerde else CorDestaque, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Text(item.statusCozinha.replace("_", " "), color = if(item.statusCozinha == "PRONTO") Color.White else CorTextoEscuro, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            // BOTÃO DE LIXEIRA PARA ITEM NO SERVIDOR
                            IconButton(onClick = { itemSendoCancelado = item.nome }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover do Servidor", tint = CorAlerta)
                            }
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // ITENS NOVOS (CARRINHO)
                if (carrinho.isNotEmpty()) {
                    item { Text("Novos Itens", fontWeight = FontWeight.Bold, color = CorTextoEscuro, modifier = Modifier.padding(bottom = 8.dp)) }
                    items(carrinho.entries.toList()) { (produto, qtd) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${qtd}X", fontWeight = FontWeight.Bold, color = CorVerde, modifier = Modifier.width(40.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(produto.nome, fontWeight = FontWeight.Bold, color = CorVerde)
                                Text("R$ ${"%.2f".format(produto.preco * qtd)}", color = CorVerde, fontSize = 12.sp)
                            }
                            IconButton(onClick = { carrinho = carrinho.toMutableMap().apply { remove(produto) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = CorAlerta)
                            }
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }

            // TOTAL E BOTÕES DE AÇÃO
            val totalServidor = comandaAtiva?.total ?: 0.0
            val totalCarrinho = carrinho.entries.sumOf { it.key.preco * it.value }

            Divider(thickness = 2.dp, modifier = Modifier.padding(vertical = 16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL:", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = CorTextoEscuro)
                Text("R$ ${"%.2f".format(totalServidor + totalCarrinho)}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = CorTextoEscuro)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // BOTÃO 1: ENVIAR PARA COZINHA (AMARELO)
            Button(
                onClick = {
                    coroutineScope.launch {
                        processando = true
                        val itensParaEnvio = carrinho.map { (prod, qtd) ->
                            com.kenji.rotisseriaadmin.data.ItemComanda(qtd, prod.nome, prod.preco, "AGUARDANDO")
                        }

                        val novaComanda = com.kenji.rotisseriaadmin.data.Comanda(
                            mesa = numeroMesa,
                            nomeCliente = nomeCliente.ifBlank { "CLIENTE" },
                            itens = itensParaEnvio,
                            total = totalCarrinho,
                            statusComanda = "EM_ABERTO",
                            dataEnvioCozinha = LocalDateTime.now().toString()
                        )

                        val (sucesso, erro) = RotisseriaApi.enviarComanda(novaComanda)
                        if (sucesso) {
                            carrinho = emptyMap()
                        } else {
                            erroConexao = erro
                        }
                        processando = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = carrinho.isNotEmpty() && !processando,
                colors = ButtonDefaults.buttonColors(backgroundColor = CorDestaque, contentColor = CorTextoEscuro)
            ) {
                if (processando) {
                    CircularProgressIndicator(color = CorTextoEscuro, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (carrinho.isNotEmpty()) "ENVIAR PARA COZINHA" else "ADICIONE ITENS", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }

            // BOTÃO 2: ENVIAR PARA O CAIXA / FECHAR CONTA (MARROM VAZADO)
            if (itensEnviados.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            processandoFechamento = true
                            val sucesso = RotisseriaApi.fecharConta(numeroMesa)
                            if (sucesso) {
                                onVoltar() // Fecha a tela da comanda e volta pra grade de mesas
                            } else {
                                erroConexao = "Erro ao enviar a conta para o caixa."
                            }
                            processandoFechamento = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CorTextoEscuro),
                    border = androidx.compose.foundation.BorderStroke(2.dp, CorTextoEscuro),
                    enabled = carrinho.isEmpty() && !processandoFechamento // Trava o botão se tiver item esquecido no carrinho
                ) {
                    if (processandoFechamento) {
                        CircularProgressIndicator(color = CorTextoEscuro, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (carrinho.isNotEmpty()) "ENVIE OS NOVOS ITENS PRIMEIRO" else "ENVIAR PARA O CAIXA",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // DIÁLOGOS (ALERTA E CANCELAMENTO)
    // ==========================================
    erroConexao?.let { mensagem ->
        AlertDialog(
            onDismissRequest = { erroConexao = null },
            title = { Text("Aviso do Servidor", color = CorAlerta, fontWeight = FontWeight.Bold) },
            text = { Text(mensagem) },
            confirmButton = {
                Button(onClick = { erroConexao = null }, colors = ButtonDefaults.buttonColors(backgroundColor = CorTextoEscuro)) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    itemSendoCancelado?.let { nomeItem ->
        AlertDialog(
            onDismissRequest = { itemSendoCancelado = null },
            title = { Text("Cancelar Item", color = CorAlerta, fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente remover o item '$nomeItem' da comanda? Ele voltará para o estoque automaticamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val sucesso = RotisseriaApi.cancelarItem(numeroMesa, nomeItem)
                            if (!sucesso) {
                                erroConexao = "Erro ao remover o item do servidor."
                            }
                            itemSendoCancelado = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = CorAlerta, contentColor = Color.White)
                ) {
                    Text("Sim, Remover", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemSendoCancelado = null }) {
                    Text("Voltar", color = CorTextoEscuro, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}