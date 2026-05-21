package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseriaadmin.data.Comanda
import com.kenji.rotisseriaadmin.data.ItemCardapioResponse
import com.kenji.rotisseriaadmin.data.ItemComanda
import com.kenji.rotisseriaadmin.data.RotisseriaApi
import com.kenji.rotisseriaadmin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun NovoPedidoScreen() {
    val coroutineScope = rememberCoroutineScope()

    // Estados do Cardápio e Carrinho
    var cardapio by remember { mutableStateOf<List<ItemCardapioResponse>>(emptyList()) }
    var carrinho by remember { mutableStateOf<Map<ItemCardapioResponse, Int>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados de Controle de Fluxo e Erros
    var mensagemSucesso by remember { mutableStateOf(false) }
    var erroConexao by remember { mutableStateOf<String?>(null) }
    var enviandoPedido by remember { mutableStateOf(false) }

    // Campos do Formulário
    var nomeCliente by remember { mutableStateOf("") }
    var isViagem by remember { mutableStateOf(false) }


    // Carregar dados iniciais
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val menuAtualizado = RotisseriaApi.buscarCardapio()
                // Só atualiza a variável se o servidor mandou algo diferente
                // Isso evita que a tela fique "piscando" ou travando
                if (cardapio != menuAtualizado) {
                    cardapio = menuAtualizado
                }
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(3000) // Aguarda 3 segundos e verifica de novo
        }
    }
    fun adicionarItem(item: ItemCardapioResponse) {
        val pratoNoCarrinho = carrinho.keys.firstOrNull { it.id == item.id }
        val qtdAtualNoCarrinho = if (pratoNoCarrinho != null) carrinho[pratoNoCarrinho] ?: 0 else 0

        // TRAVA: Se tem estoque, não deixa passar do limite
        if (item.estoqueAtual != null) {
            if (qtdAtualNoCarrinho >= item.estoqueAtual) {
                return // Cancela a ação de adicionar
            }
        }

        carrinho = carrinho.toMutableMap().apply {
            if (pratoNoCarrinho != null) remove(pratoNoCarrinho)
            put(item, qtdAtualNoCarrinho + 1)
        }
    }

    fun removerItem(item: ItemCardapioResponse) {
        val pratoNoCarrinho = carrinho.keys.firstOrNull { it.id == item.id }
        if (pratoNoCarrinho != null) {
            val qtd = carrinho[pratoNoCarrinho] ?: 0
            carrinho = carrinho.toMutableMap().apply {
                remove(pratoNoCarrinho)
                if (qtd > 1) put(item, qtd - 1)
            }
        }
    }
    fun atualizarDadosCardapio() {
        coroutineScope.launch {
            cardapio = RotisseriaApi.buscarCardapio()
        }
    }

    val totalCarrinho = carrinho.entries.sumOf { it.key.preco * it.value }

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (isLoading) {
            CircularProgressIndicator(color = PrimaryBrown, modifier = Modifier.align(Alignment.Center))
        } else if (mensagemSucesso) {
            // FEEDBACK DE SUCESSO
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✅ PEDIDO ENVIADO PARA A COZINHA!", color = CorVerde, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { mensagemSucesso = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBrown, contentColor = OnPrimary)
                ) {
                    Text("REALIZAR OUTRO PEDIDO", modifier = Modifier.padding(8.dp))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                // COLUNA DA ESQUERDA: LISTA DE PRODUTOS
                Column(modifier = Modifier.weight(1.3f)) {
                    Text("Novo Pedido", color = TextDarkBrown, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // ADICIONE O '.filter { it.disponivel }' AQUI:
                        val categorias = cardapio.filter { it.disponivel }.groupBy { it.categoria }

                        categorias.forEach { (cat, itens) ->
                            item {
                                Text(cat.uppercase(), color = PrimaryBrown, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                            }
                            items(itens) { prato ->
                                // Calcula quanto ainda sobra depois do que já está no carrinho
                                val qtdNoCarrinho = carrinho.entries.firstOrNull { it.key.id == prato.id }?.value ?: 0
                                val estoqueRestante = prato.estoqueAtual?.let { it - qtdNoCarrinho }
                                val isEsgotado = estoqueRestante != null && estoqueRestante <= 0

                                Card(
                                    backgroundColor = SurfaceWhite,
                                    elevation = 1.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // Bloqueia o clique se esgotar e deixa o card mais "apagado"
                                        .clickable(enabled = !isEsgotado) { adicionarItem(prato) }
                                        .alpha(if (isEsgotado) 0.5f else 1f)
                                ) {
                                    Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(prato.nome, color = TextDarkBrown, fontSize = 18.sp, fontWeight = FontWeight.Medium)

                                            // Exibe o estoque atualizado ou o aviso de ESGOTADO
                                            if (estoqueRestante != null) {
                                                Text(
                                                    text = if (isEsgotado) "ESGOTADO" else "Disponível: $estoqueRestante",
                                                    color = if (isEsgotado) CorAlerta else TextDarkBrown.copy(alpha = 0.5f),
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isEsgotado) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                        Text("R$ ${"%.2f".format(prato.preco)}", color = SecondaryOrange, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // COLUNA DA DIREITA: CARRINHO E ENVIO
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("RESUMO DO PEDIDO", color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))

                        if (carrinho.isEmpty()) {
                            Text("Carrinho vazio. Clique nos pratos para adicionar.", color = TextDarkBrown.copy(alpha = 0.4f), fontSize = 16.sp)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(carrinho.entries.toList()) { (prato, qtd) ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prato.nome, color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                            Text("R$ ${"%.2f".format(prato.preco * qtd)}", color = SecondaryOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { removerItem(prato) }) {
                                                Icon(Icons.Default.Remove, tint = PrimaryBrown, contentDescription = "remover")
                                            }

                                            Text(qtd.toString(), color = TextDarkBrown, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                                            // Verifica no cardápio mais recente (do servidor) qual é o limite real
                                            val pratoAtualizado = cardapio.firstOrNull { it.id == prato.id }
                                            val podeAdicionarMais = pratoAtualizado?.estoqueAtual == null || qtd < pratoAtualizado.estoqueAtual

                                            IconButton(
                                                onClick = { if (podeAdicionarMais) adicionarItem(prato) },
                                                enabled = podeAdicionarMais
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    // Fica cinza clarinho se não puder adicionar mais
                                                    tint = if (podeAdicionarMais) PrimaryBrown else TextDarkBrown.copy(alpha = 0.3f),
                                                    contentDescription = "adicionar"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // FORMULÁRIO DE FECHAMENTO
                    Column {
                        Divider(color = SurfaceWhite, thickness = 2.dp, modifier = Modifier.padding(vertical = 16.dp))

                        OutlinedTextField(
                            value = nomeCliente,
                            onValueChange = { nomeCliente = it },
                            label = { Text("Nome do Cliente / Mesa") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = PrimaryBrown, focusedLabelColor = PrimaryBrown, cursorColor = PrimaryBrown, textColor = TextDarkBrown),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isViagem = !isViagem }.padding(vertical = 8.dp)) {
                            Checkbox(checked = isViagem, onCheckedChange = { isViagem = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryBrown))
                            Text("Pedido para Viagem / Entrega", color = TextDarkBrown, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("TOTAL:", color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("R$ ${"%.2f".format(totalCarrinho)}", color = PrimaryBrown, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // BOTÃO DE ENVIO PARA COZINHA
                        Button(
                            onClick = {
                                if (carrinho.isNotEmpty() && nomeCliente.isNotBlank()) {
                                    coroutineScope.launch {
                                        enviandoPedido = true
                                        erroConexao = null

                                        val horarioEnvio = java.time.LocalDateTime.now().toString()

                                        val itensComanda = carrinho.map { (prato, qtd) ->
                                            ItemComanda(
                                                quantidade = qtd,
                                                nome = prato.nome,
                                                preco = prato.preco,
                                                statusCozinha = "AGUARDANDO",
                                                categoria = prato.categoria,
                                                parceria = prato.parceria
                                            )
                                        }

                                        val identificadorMesa = if (isViagem) {
                                            "VIAGEM: ${nomeCliente.trim().uppercase()}"
                                        } else {
                                            "BALCAO: ${nomeCliente.trim().uppercase()}"
                                        }

                                        val novaComanda = Comanda(
                                            mesa = identificadorMesa,
                                            nomeCliente = nomeCliente.trim().uppercase(),
                                            itens = itensComanda,
                                            total = totalCarrinho,
                                            statusComanda = "EM_ABERTO",
                                            dataEnvioCozinha = horarioEnvio
                                        )

                                        // MUDANÇA AQUI: Agora recebemos se deu sucesso e qual foi o erro
                                        val (sucesso, mensagemErro) = RotisseriaApi.enviarComanda(novaComanda)

                                        if (sucesso) {
                                            atualizarDadosCardapio()
                                            carrinho = emptyMap()
                                            nomeCliente = ""
                                            isViagem = false
                                            mensagemSucesso = true
                                        } else {
                                            // Se falhou, joga a mensagem exata do servidor no pop-up
                                            erroConexao = mensagemErro
                                            // E força uma atualização da tela para o garçom ver o estoque real que sobrou
                                            atualizarDadosCardapio()
                                        }
                                        enviandoPedido = false
                                    }
                                }
                            },
                            enabled = carrinho.isNotEmpty() && nomeCliente.isNotBlank() && !enviandoPedido,
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBrown, contentColor = OnPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (enviandoPedido) {
                                CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Text("ENVIAR PARA COZINHA", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        // POP-UP DE ALERTA CASO A REDE FALHE
        erroConexao?.let { mensagem ->
            AlertDialog(
                onDismissRequest = { erroConexao = null },
                title = { Text("Erro de Comunicação", color = CorAlerta, fontWeight = FontWeight.Bold) },
                text = { Text(mensagem, color = TextDarkBrown) },
                confirmButton = {
                    TextButton(onClick = { erroConexao = null }) {
                        Text("ENTENDIDO", color = PrimaryBrown, fontWeight = FontWeight.Bold)
                    }
                },
                backgroundColor = SurfaceWhite
            )
        }
    }
}