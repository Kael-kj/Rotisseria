package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    // Campos do Formulário
    var metodoPagamento by remember { mutableStateOf("Pix") }
    var valorDescontoStr by remember { mutableStateOf("") }
    var nomeFiadoCustomizado by remember { mutableStateOf("") } // Nome que irá para a caderneta

    // Função para carregar comandas e filtrar apenas o que deve aparecer no caixa
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

                // Só atualiza a tela se tiver pedido novo ou mudança de status
                if (comandasAbertas != filtradas) {
                    comandasAbertas = filtradas
                    // Limpa a seleção se a comanda foi paga em outro dispositivo
                    if (comandaSelecionada != null && filtradas.none { it.mesa == comandaSelecionada?.mesa }) {
                        comandaSelecionada = null
                    }
                }
            } catch (e: Exception) {
                if (isPrimeiraCarga) erroConexao = "Erro ao carregar dados do servidor."
            }
            if (isPrimeiraCarga) isLoading = false
        }
    }

    // O Loop de Polling (Substitua o seu LaunchedEffect atual por este)
    LaunchedEffect(Unit) {
        carregarComandas(isPrimeiraCarga = true)
        while (true) {
            kotlinx.coroutines.delay(3000) // Verifica pedidos novos a cada 3 segundos
            carregarComandas(isPrimeiraCarga = false)
        }
    }

    // Mantém este que já estava no seu código para o Fiado
    LaunchedEffect(comandaSelecionada) {
        nomeFiadoCustomizado = comandaSelecionada?.nomeCliente ?: ""
    }

    // Cálculos de Total
    val totalOriginal = comandaSelecionada?.total ?: 0.0
    val desconto = valorDescontoStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val totalComDesconto = (totalOriginal - desconto).coerceAtLeast(0.0)

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (isLoading) {
            CircularProgressIndicator(color = PrimaryBrown, modifier = Modifier.align(Alignment.Center))
        } else if (mensagemSucesso) {
            // FEEDBACK DE SUCESSO
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✅ PAGAMENTO REGISTRADO!", color = CorVerde, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        mensagemSucesso = false
                        comandaSelecionada = null
                        valorDescontoStr = ""
                        carregarComandas()
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBrown, contentColor = OnPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("VOLTAR PARA O CAIXA", modifier = Modifier.padding(8.dp))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                // ==========================================
                // COLUNA DA ESQUERDA: LISTA DE COMANDAS
                // ==========================================
                Column(modifier = Modifier.weight(1.3f)) {
                    Text("Contas em Aberto", color = TextDarkBrown, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (comandasAbertas.isEmpty()) {
                        Text("Nenhuma conta aguardando pagamento.", color = TextDarkBrown.copy(alpha = 0.5f), fontSize = 16.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(comandasAbertas) { comanda ->
                                val isSelecionada = comanda.mesa == comandaSelecionada?.mesa
                                Card(
                                    backgroundColor = if (isSelecionada) BackgroundCream else SurfaceWhite,
                                    elevation = if (isSelecionada) 4.dp else 1.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        comandaSelecionada = comanda
                                        valorDescontoStr = ""
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(comanda.mesa, color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Text("Cliente: ${comanda.nomeCliente}", color = TextDarkBrown.copy(alpha = 0.7f), fontSize = 14.sp)
                                        }
                                        Text("R$ ${"%.2f".format(comanda.total)}", color = SecondaryOrange, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                            Text("Selecione uma comanda ao lado", color = TextDarkBrown.copy(alpha = 0.3f), fontSize = 18.sp)
                        }
                    } else {
                        val comanda = comandaSelecionada!!

                        Column(modifier = Modifier.weight(1f)) {
                            Text("RESUMO DA CONTA", color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("${comanda.mesa} - ${comanda.nomeCliente}", color = PrimaryBrown, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(24.dp))

                            // LISTA DE ITENS
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(comanda.itens) { item ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${item.quantidade}x ${item.nome}", color = TextDarkBrown, fontSize = 15.sp)
                                        Text("R$ ${"%.2f".format(item.preco * item.quantidade)}", color = TextDarkBrown.copy(alpha = 0.6f), fontSize = 15.sp)
                                    }
                                }
                            }
                        }

                        // ÁREA DE PAGAMENTO E DESCONTO
                        Column {
                            Divider(color = SurfaceWhite, thickness = 2.dp, modifier = Modifier.padding(vertical = 16.dp))

                            OutlinedTextField(
                                value = valorDescontoStr,
                                onValueChange = { valorDescontoStr = it },
                                label = { Text("Valor do Desconto (R$)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = PrimaryBrown, cursorColor = PrimaryBrown),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("MÉTODO DE PAGAMENTO:", color = TextDarkBrown, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("Dinheiro", "Pix", "Cartão", "Fiado").forEach { metodo ->
                                    val isAtivo = metodoPagamento == metodo
                                    Box(
                                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                            .background(if (isAtivo) (if(metodo == "Fiado") CorAlerta else PrimaryBrown) else SurfaceWhite)
                                            .clickable { metodoPagamento = metodo }.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(metodo, color = if (isAtivo) OnPrimary else TextDarkBrown, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // CAMPO DE NOME PERSONALIZADO PARA FIADO
                            if (metodoPagamento == "Fiado") {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = nomeFiadoCustomizado,
                                    onValueChange = { nomeFiadoCustomizado = it },
                                    label = { Text("Nome para a Caderneta") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = CorAlerta, cursorColor = CorAlerta),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("TOTAL FINAL:", color = TextDarkBrown, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Column(horizontalAlignment = Alignment.End) {
                                    if (desconto > 0) {
                                        Text(
                                            text = "R$ ${"%.2f".format(totalOriginal)}",
                                            color = TextDarkBrown.copy(alpha = 0.4f),
                                            fontSize = 14.sp,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    }
                                    Text("R$ ${"%.2f".format(totalComDesconto)}", color = if(metodoPagamento == "Fiado") CorAlerta else SecondaryOrange, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        processandoPagamento = true

                                        // Decide qual nome enviar: o original ou o que você digitou para o fiado
                                        val nomeParaEnviar = if (metodoPagamento == "Fiado") nomeFiadoCustomizado else comanda.nomeCliente

                                        val sucesso = RotisseriaApi.confirmarPagamento(
                                            mesa = comanda.mesa,
                                            metodo = metodoPagamento.uppercase(),
                                            valorFinal = totalComDesconto,
                                            nomeFiado = nomeParaEnviar.trim().uppercase()
                                        )

                                        if (sucesso) {
                                            // Remove da lista local imediatamente
                                            comandasAbertas = comandasAbertas.filter { it.mesa != comanda.mesa }
                                            mensagemSucesso = true
                                        } else {
                                            erroConexao = "Erro ao processar pagamento no servidor."
                                        }
                                        processandoPagamento = false
                                    }
                                },
                                enabled = !processandoPagamento && (metodoPagamento != "Fiado" || nomeFiadoCustomizado.isNotBlank()),
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if(metodoPagamento == "Fiado") CorAlerta else PrimaryBrown,
                                    contentColor = OnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (processandoPagamento) {
                                    CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = if(metodoPagamento == "Fiado") "REGISTRAR NA CADERNETA" else "CONFIRMAR PAGAMENTO",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // POP-UP DE ALERTA
        erroConexao?.let { mensagem ->
            AlertDialog(
                onDismissRequest = { erroConexao = null },
                title = { Text("Erro", color = CorAlerta, fontWeight = FontWeight.Bold) },
                text = { Text(mensagem) },
                confirmButton = { TextButton(onClick = { erroConexao = null }) { Text("OK", color = PrimaryBrown) } },
                backgroundColor = SurfaceWhite
            )
        }
    }
}