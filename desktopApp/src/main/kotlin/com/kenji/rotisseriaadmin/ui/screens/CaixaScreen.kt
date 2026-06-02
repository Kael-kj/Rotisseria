package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseriaadmin.data.ComandaResponse
import com.kenji.rotisseriaadmin.data.RotisseriaApi
import com.kenji.rotisseriaadmin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CaixaScreen() {
    val coroutineScope = rememberCoroutineScope()

    // Estados dos Dados
    var comandasAbertas by remember { mutableStateOf<List<ComandaResponse>>(emptyList()) }
    var comandaSelecionada by remember { mutableStateOf<ComandaResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados de Controle de Fluxo
    var processandoPagamento by remember { mutableStateOf(false) }
    var mensagemSucesso by remember { mutableStateOf(false) }
    var erroConexao by remember { mutableStateOf<String?>(null) }
    var itemParaCancelar by remember { mutableStateOf<Pair<String, String>?>(null) } // Guarda Mesa e Nome do Item

    // Campos do Formulário
    var metodoPagamento by remember { mutableStateOf("Pix") }
    var valorDescontoStr by remember { mutableStateOf("") }
    var nomeFiadoCustomizado by remember { mutableStateOf("") }

    fun carregarComandas(isPrimeiraCarga: Boolean = false) {
        coroutineScope.launch {
            if (isPrimeiraCarga) isLoading = true
            try {
                val todas = RotisseriaApi.buscarContasAbertas()

                val filtradas = todas.filter { comanda ->
                    val mesaUpper = comanda.mesa.uppercase()
                    comanda.statusComanda == "A_PAGAR" ||
                            mesaUpper.contains("BALCAO") || mesaUpper.contains("VIAGEM")
                }.groupBy { it.mesa }.map { (mesa, listaDePedidos) ->
                    ComandaResponse(
                        mesa = mesa,
                        nomeCliente = listaDePedidos.first().nomeCliente,
                        itens = listaDePedidos.flatMap { it.itens },
                        total = listaDePedidos.sumOf { it.total },
                        statusComanda = if (listaDePedidos.any { it.statusComanda == "A_PAGAR" }) "A_PAGAR" else "EM_ABERTO",
                        dataEnvioCozinha = listaDePedidos.firstOrNull()?.dataEnvioCozinha
                    )
                }

                if (comandasAbertas != filtradas) {
                    comandasAbertas = filtradas
                    // Atualiza a comanda selecionada com os dados novos (novo total e nova lista de itens)
                    if (comandaSelecionada != null) {
                        comandaSelecionada = filtradas.find { it.mesa == comandaSelecionada?.mesa }
                    }
                }
            } catch (e: Exception) {
                if (isPrimeiraCarga) erroConexao = "Erro ao carregar dados do servidor."
            }
            if (isPrimeiraCarga) isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        carregarComandas(isPrimeiraCarga = true)
        while (true) {
            kotlinx.coroutines.delay(3000)
            carregarComandas(isPrimeiraCarga = false)
        }
    }

    LaunchedEffect(comandaSelecionada) {
        nomeFiadoCustomizado = comandaSelecionada?.nomeCliente ?: ""
    }

    val totalOriginal = comandaSelecionada?.total ?: 0.0
    val desconto = valorDescontoStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val totalComDesconto = (totalOriginal - desconto).coerceAtLeast(0.0)

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (isLoading) {
            CircularProgressIndicator(color = PrimaryBrown, modifier = Modifier.align(Alignment.Center))
        } else if (mensagemSucesso) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✅ PAGAMENTO REGISTRADO!", color = CorVerde, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        mensagemSucesso = false
                        comandaSelecionada = null
                        valorDescontoStr = ""
                        carregarComandas()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBrown, contentColor = OnPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("VOLTAR PARA O CAIXA", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                // ==========================================
                // COLUNA DA ESQUERDA: LISTA DE COMANDAS
                // ==========================================
                Column(modifier = Modifier.weight(1.3f)) {
                    Text("Contas em Aberto", color = TextDarkBrown, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (comandasAbertas.isEmpty()) {
                        Text("Nenhuma conta aguardando pagamento.", color = TextDarkBrown, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(comandasAbertas) { comanda ->
                                val isSelecionada = comanda.mesa == comandaSelecionada?.mesa
                                Card(
                                    backgroundColor = if (isSelecionada) BackgroundCream else SurfaceWhite,
                                    elevation = if (isSelecionada) 8.dp else 2.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        comandaSelecionada = comanda
                                        valorDescontoStr = ""
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(comanda.mesa, color = TextDarkBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Cliente: ${comanda.nomeCliente}", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Text("R$ ${"%.2f".format(comanda.total)}", color = if (isSelecionada) SecondaryOrange else TextDarkBrown, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // COLUNA DA DIREITA: DETALHES E FECHAMENTO
                // ==========================================
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    if (comandaSelecionada == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Selecione uma comanda na lista", color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        val comanda = comandaSelecionada!!

                        Column(modifier = Modifier.weight(1f)) {
                            Text("RESUMO DA CONTA", color = TextDarkBrown, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text("${comanda.mesa} - ${comanda.nomeCliente}", color = PrimaryBrown, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))

                            // LISTA DE ITENS COM BOTÃO DE EXCLUIR
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(comanda.itens) { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${item.quantidade}x ${item.nome}", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                            Text("R$ ${"%.2f".format(item.preco * item.quantidade)}", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(onClick = { itemParaCancelar = Pair(comanda.mesa, item.nome) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remover Item",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ÁREA DE PAGAMENTO E DESCONTO
                        Column {
                            Divider(color = TextDarkBrown.copy(alpha = 0.1f), thickness = 2.dp, modifier = Modifier.padding(vertical = 20.dp))

                            OutlinedTextField(
                                value = valorDescontoStr,
                                onValueChange = { valorDescontoStr = it },
                                label = { Text("Valor do Desconto (R$)", fontWeight = FontWeight.Medium) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = TextDarkBrown,
                                    focusedBorderColor = PrimaryBrown,
                                    cursorColor = PrimaryBrown
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text("MÉTODO DE PAGAMENTO:", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("Dinheiro", "Pix", "Cartão", "Fiado").forEach { metodo ->
                                    val isAtivo = metodoPagamento == metodo
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                            .background(if (isAtivo) (if(metodo == "Fiado") CorAlerta else PrimaryBrown) else SurfaceWhite)
                                            .clickable { metodoPagamento = metodo }.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(metodo, color = if (isAtivo) OnPrimary else TextDarkBrown, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (metodoPagamento == "Fiado") {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = nomeFiadoCustomizado,
                                    onValueChange = { nomeFiadoCustomizado = it },
                                    label = { Text("Nome para a Caderneta", fontWeight = FontWeight.Medium) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        textColor = TextDarkBrown,
                                        focusedBorderColor = CorAlerta,
                                        cursorColor = CorAlerta
                                    ),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("TOTAL FINAL:", color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Column(horizontalAlignment = Alignment.End) {
                                    if (desconto > 0) {
                                        Text(
                                            text = "R$ ${"%.2f".format(totalOriginal)}",
                                            color = CorAlerta,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    }
                                    Text("R$ ${"%.2f".format(totalComDesconto)}", color = if(metodoPagamento == "Fiado") CorAlerta else SecondaryOrange, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        processandoPagamento = true
                                        val nomeParaEnviar = if (metodoPagamento == "Fiado") nomeFiadoCustomizado else comanda.nomeCliente
                                        val sucesso = RotisseriaApi.confirmarPagamento(
                                            mesa = comanda.mesa,
                                            metodo = metodoPagamento.uppercase(),
                                            valorFinal = totalComDesconto,
                                            nomeFiado = nomeParaEnviar.trim().uppercase()
                                        )
                                        if (sucesso) {
                                            comandasAbertas = comandasAbertas.filter { it.mesa != comanda.mesa }
                                            mensagemSucesso = true
                                        } else {
                                            erroConexao = "Erro ao processar pagamento no servidor."
                                        }
                                        processandoPagamento = false
                                    }
                                },
                                enabled = !processandoPagamento && (metodoPagamento != "Fiado" || nomeFiadoCustomizado.isNotBlank()),
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if(metodoPagamento == "Fiado") CorAlerta else PrimaryBrown,
                                    contentColor = OnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (processandoPagamento) {
                                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(28.dp))
                                } else {
                                    Text(
                                        text = if(metodoPagamento == "Fiado") "REGISTRAR NA CADERNETA" else "CONFIRMAR PAGAMENTO",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // DIÁLOGOS DE ALERTA E ERRO
        // ==========================================

        // Modal de confirmação para cancelar item
        itemParaCancelar?.let { (mesa, nomeItem) ->
            AlertDialog(
                onDismissRequest = { itemParaCancelar = null },
                title = { Text("Cancelar Item", color = CorAlerta, fontWeight = FontWeight.Bold) },
                text = { Text("Deseja realmente remover o item '$nomeItem' da comanda? Ele voltará para o estoque automaticamente.", fontWeight = FontWeight.Medium) },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val sucesso = RotisseriaApi.cancelarItem(mesa, nomeItem)
                                if (sucesso) {
                                    carregarComandas(isPrimeiraCarga = true) // Recarrega para atualizar a interface com o novo total
                                } else {
                                    erroConexao = "Erro ao remover o item. Verifique a conexão."
                                }
                                itemParaCancelar = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White)
                    ) {
                        Text("Sim, Remover", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemParaCancelar = null }) {
                        Text("Voltar", color = PrimaryBrown, fontWeight = FontWeight.Bold)
                    }
                },
                backgroundColor = SurfaceWhite
            )
        }

        // Modal de Erro Genérico
        erroConexao?.let { mensagem ->
            AlertDialog(
                onDismissRequest = { erroConexao = null },
                title = { Text("Erro", color = CorAlerta, fontWeight = FontWeight.Bold) },
                text = { Text(mensagem, fontWeight = FontWeight.Medium) },
                confirmButton = { TextButton(onClick = { erroConexao = null }) { Text("OK", color = PrimaryBrown, fontWeight = FontWeight.Bold) } },
                backgroundColor = SurfaceWhite
            )
        }
    }
}