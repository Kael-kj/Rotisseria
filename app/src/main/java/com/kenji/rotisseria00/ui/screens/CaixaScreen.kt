package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kenji.rotisseria00.models.Comanda
import com.kenji.rotisseria00.network.RotisseriaApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaixaScreen() {
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)
    val corFundoCard = Color(0xFF362511)
    val corAlerta = Color(0xFFD32F2F)

    val coroutineScope = rememberCoroutineScope()
    var comandasPendentes by remember { mutableStateOf<List<Comanda>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estado do Modal de Pagamento
    var comandaSelecionada by remember { mutableStateOf<Comanda?>(null) }
    var tipoDesconto by remember { mutableStateOf("R$") }
    var valorDescontoStr by remember { mutableStateOf("") }
    var metodoSelecionado by remember { mutableStateOf<String?>(null) }
    var finalizandoPagamento by remember { mutableStateOf(false) }

    // Estados novos para o Fiado
    var mostrarCampoNomeFiado by remember { mutableStateOf(false) }
    var nomeFiado by remember { mutableStateOf("") }
    var menuFiadoExpandido by remember { mutableStateOf(false) }

    fun carregarContas() {
        coroutineScope.launch {
            isLoading = true
            comandasPendentes = RotisseriaApi.buscarContasPendentes()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { carregarContas() }

    val nomesFiadosExistentes = comandasPendentes.mapNotNull { it.nomeCliente }.filter { it.isNotBlank() }.distinct()

    Scaffold(containerColor = corFundoApp) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp)) {
            Text("CAIXA - AGUARDANDO PAGAMENTO", color = corTextoDestaque, fontSize = 32.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = corTextoDestaque) }
            } else if (comandasPendentes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma conta aguardando pagamento.", color = corTextoClaro.copy(alpha = 0.5f), fontSize = 20.sp, fontFamily = FidalgaFont)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(comandasPendentes) { comanda ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = corFundoCard), shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                comandaSelecionada = comanda
                                valorDescontoStr = ""
                                tipoDesconto = "R$"
                                metodoSelecionado = null
                                mostrarCampoNomeFiado = false
                                nomeFiado = comanda.nomeCliente ?: ""
                            }
                        ) {
                            Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("MESA ${comanda.mesa}", color = corTextoDestaque, fontSize = 24.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                    if (!comanda.nomeCliente.isNullOrBlank()) {
                                        Text("Cliente: ${comanda.nomeCliente}", color = corTextoClaro, fontSize = 16.sp, fontFamily = FidalgaFont)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total a Pagar", color = corTextoClaro.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = FidalgaFont)
                                        Text("R$ ${"%.2f".format(comanda.total)}", color = corTextoDestaque, fontSize = 28.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(24.dp))
                                    Icon(Icons.Default.PointOfSale, contentDescription = "Pagar", tint = corTextoClaro, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // MODAL DE COBRANÇA DIVIDIDO (SIDE-BY-SIDE)
    // ==========================================

    // A MÁGICA ESTÁ AQUI: Usamos 'let' para criar uma cópia segura (comandaModal)
    comandaSelecionada?.let { comandaModal ->

        val valorOriginal = comandaModal.total
        val valorDescontoNum = valorDescontoStr.replace(",", ".").toDoubleOrNull() ?: 0.0

        val valorFinal = if (tipoDesconto == "R$") {
            maxOf(0.0, valorOriginal - valorDescontoNum)
        } else {
            maxOf(0.0, valorOriginal - (valorOriginal * (valorDescontoNum / 100.0)))
        }

        Dialog(
            onDismissRequest = { if (!finalizandoPagamento) comandaSelecionada = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 700.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize().background(corFundoCard)) {

                    // --- COLUNA ESQUERDA: RESUMO DA COMANDA ---
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF5F5F5)).padding(24.dp)
                    ) {
                        Text("RESUMO DO CONSUMO", color = Color.DarkGray, fontSize = 20.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                        Text("Mesa ${comandaModal.mesa}", color = Color.Gray, fontSize = 16.sp, fontFamily = FidalgaFont) // Sem !!

                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(comandaModal.itens) { item -> // Sem !!
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${item.quantidade}x ${item.nome}", color = Color.Black, fontSize = 16.sp, fontFamily = FidalgaFont)
                                    Text("R$ ${"%.2f".format(item.preco * item.quantidade)}", color = Color.Black, fontSize = 16.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // --- COLUNA DIREITA: CAIXA E PAGAMENTO ---
                    Column(
                        modifier = Modifier.weight(1.2f).fillMaxHeight().padding(32.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("FINALIZAR CONTA", color = corTextoDestaque, fontSize = 28.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(32.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Total da Comanda:", color = corTextoClaro.copy(alpha = 0.7f), fontSize = 18.sp, fontFamily = FidalgaFont)
                                Text("R$ ${"%.2f".format(valorOriginal)}", color = corTextoClaro, fontSize = 24.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("Aplicar Desconto:", color = corTextoClaro, fontSize = 16.sp, fontFamily = FidalgaFont)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { tipoDesconto = if (tipoDesconto == "R$") "%" else "R$" },
                                    colors = ButtonDefaults.buttonColors(containerColor = corFundoApp, contentColor = corTextoDestaque),
                                    modifier = Modifier.height(56.dp)
                                ) { Text(tipoDesconto, fontSize = 20.sp, fontWeight = FontWeight.Bold) }

                                OutlinedTextField(
                                    value = valorDescontoStr, onValueChange = { valorDescontoStr = it },
                                    placeholder = { Text("0.00", color = corTextoClaro.copy(alpha = 0.5f)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque),
                                    modifier = Modifier.weight(1f).height(60.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("A COBRAR:", color = corTextoClaro, fontSize = 20.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                Text("R$ ${"%.2f".format(valorFinal)}", color = Color(0xFF388E3C), fontSize = 42.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                            }
                        }

                        // --- MÉTODOS DE PAGAMENTO OU TELA DE FIADO ---
                        Column {
                            if (finalizandoPagamento) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = corTextoDestaque) }
                            } else if (mostrarCampoNomeFiado) {
                                Text("CLIENTE DO FIADO", color = corTextoClaro.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = FidalgaFont)
                                Spacer(modifier = Modifier.height(16.dp))

                                ExposedDropdownMenuBox(
                                    expanded = menuFiadoExpandido,
                                    onExpandedChange = { menuFiadoExpandido = it }
                                ) {
                                    OutlinedTextField(
                                        value = nomeFiado,
                                        onValueChange = { nomeFiado = it; menuFiadoExpandido = true },
                                        placeholder = { Text("Nome do Cliente", color = corTextoClaro.copy(alpha = 0.5f), fontFamily = FidalgaFont) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque),
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(fontFamily = FidalgaFont, fontSize = 18.sp)
                                    )
                                    if (nomesFiadosExistentes.isNotEmpty()) {
                                        ExposedDropdownMenu(expanded = menuFiadoExpandido, onDismissRequest = { menuFiadoExpandido = false }, modifier = Modifier.background(corFundoApp)) {
                                            nomesFiadosExistentes.forEach { nome ->
                                                DropdownMenuItem(
                                                    text = { Text(nome, color = corTextoClaro, fontFamily = FidalgaFont) },
                                                    onClick = { nomeFiado = nome; menuFiadoExpandido = false }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextButton(onClick = { mostrarCampoNomeFiado = false }, modifier = Modifier.weight(1f).height(64.dp)) {
                                        Text("VOLTAR", color = corTextoClaro, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            if (nomeFiado.isNotBlank()) {
                                                finalizandoPagamento = true
                                                coroutineScope.launch {
                                                    val sucesso = RotisseriaApi.confirmarPagamento(comandaModal.mesa, "FIADO", valorFinal, nomeFiado)
                                                    if (sucesso) {
                                                        comandaSelecionada = null
                                                        mostrarCampoNomeFiado = false
                                                        carregarContas()
                                                    }
                                                    finalizandoPagamento = false
                                                }
                                            }
                                        },
                                        enabled = nomeFiado.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = corAlerta, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1.5f).height(64.dp)
                                    ) { Text("SALVAR FIADO", fontFamily = FidalgaFont, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                                }

                            } else {
                                Text("MÉTODO DE PAGAMENTO", color = corTextoClaro.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = FidalgaFont)
                                Spacer(modifier = Modifier.height(16.dp))

                                val metodos = listOf("PIX", "DINHEIRO", "DÉBITO", "CRÉDITO")

                                metodos.chunked(2).forEach { par ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        par.forEach { metodo ->
                                            val isSelected = metodoSelecionado == metodo
                                            Button(
                                                onClick = { metodoSelecionado = metodo },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) corTextoDestaque else corFundoApp,
                                                    contentColor = if (isSelected) corFundoCard else corTextoClaro
                                                ),
                                                shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(56.dp)
                                            ) { Text(metodo, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            if (metodoSelecionado != null) {
                                                finalizandoPagamento = true
                                                coroutineScope.launch {
                                                    val sucesso = RotisseriaApi.confirmarPagamento(comandaModal.mesa, metodoSelecionado!!, valorFinal, null)
                                                    if (sucesso) {
                                                        comandaSelecionada = null
                                                        carregarContas()
                                                    }
                                                    finalizandoPagamento = false
                                                }
                                            }
                                        },
                                        enabled = metodoSelecionado != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C), contentColor = Color.White, disabledContainerColor = Color.Gray),
                                        shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1.5f).height(64.dp)
                                    ) { Text("CONFIRMAR PAGAMENTO", fontFamily = FidalgaFont, fontWeight = FontWeight.Bold, fontSize = 14.sp) }

                                    Button(
                                        onClick = { mostrarCampoNomeFiado = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = corAlerta, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(64.dp)
                                    ) { Text("MARCAR FIADO", fontFamily = FidalgaFont, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { comandaSelecionada = null },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("CANCELAR", color = corTextoClaro, fontFamily = FidalgaFont) }
                            }
                        }
                    }
                }
            }
        }
    }
}