package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseria00.models.ItemCardapioRequest
import com.kenji.rotisseria00.models.ItemCardapioResponse
import com.kenji.rotisseria00.network.RotisseriaApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardapioScreen() {
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)
    val corFundoCard = Color(0xFF362511)
    val corDivisor = Color(0xFF5A4A32)

    val coroutineScope = rememberCoroutineScope()
    var listaCardapio by remember { mutableStateOf<List<ItemCardapioResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Controle de Edição
    var pratoEmEdicao by remember { mutableStateOf<ItemCardapioResponse?>(null) }

    // Estados do Formulário Fixo na tela
    var nome by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var precoStr by remember { mutableStateOf("") }
    var parceria by remember { mutableStateOf("Nenhuma") }
    var menuExpandido by remember { mutableStateOf(false) }

    // NOVOS ESTADOS: Controle do Prato do Dia (Fase 2)
    var temLimiteDiario by remember { mutableStateOf(false) }
    var limiteDiarioInput by remember { mutableStateOf("") }

    // Atualiza os campos do formulário sempre que um prato for selecionado para edição
    LaunchedEffect(pratoEmEdicao) {
        nome = pratoEmEdicao?.nome ?: ""
        categoria = pratoEmEdicao?.categoria ?: ""
        precoStr = pratoEmEdicao?.preco?.toString() ?: ""
        parceria = pratoEmEdicao?.parceria ?: "Nenhuma"

        // Puxa o limite se ele existir
        temLimiteDiario = pratoEmEdicao?.limiteDiario != null
        limiteDiarioInput = pratoEmEdicao?.limiteDiario?.toString() ?: ""
    }

    fun carregarCardapio() {
        coroutineScope.launch {
            isLoading = true
            listaCardapio = RotisseriaApi.buscarCardapio()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { carregarCardapio() }

    val cardapioAgrupado = listaCardapio.groupBy { it.categoria }
    val categoriasExistentes = listaCardapio.map { it.categoria }.distinct().sorted()

    Scaffold(
        containerColor = corFundoApp,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pratoEmEdicao = null
                    nome = ""
                    categoria = ""
                    precoStr = ""
                    parceria = "Nenhuma"
                    temLimiteDiario = false
                    limiteDiarioInput = ""
                },
                containerColor = corTextoDestaque,
                contentColor = corFundoCard,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Novo Prato", modifier = Modifier.size(32.dp)) }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(32.dp)) {
            Text("GESTÃO DE CARDÁPIO", color = corTextoDestaque, fontSize = 32.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = corTextoDestaque) }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {

                    // --- COLUNA ESQUERDA: LISTA DO CARDÁPIO ---
                    Column(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                        if (listaCardapio.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Nenhum prato cadastrado.", color = corTextoClaro.copy(alpha = 0.5f), fontSize = 20.sp, fontFamily = FidalgaFont)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                                cardapioAgrupado.forEach { (cat, itens) ->
                                    item {
                                        Text("$cat (${itens.size})", color = corTextoClaro, fontSize = 20.sp, fontFamily = FidalgaFont, modifier = Modifier.padding(vertical = 16.dp))
                                    }
                                    items(itens) { produto ->
                                        CardProdutoItem(
                                            produto = produto, corFundo = corFundoCard, corDestaque = corTextoDestaque, corClara = corTextoClaro,
                                            isSelecionado = pratoEmEdicao?.id == produto.id,
                                            onEditClick = { pratoEmEdicao = produto },
                                            onToggleChange = { novoStatus ->
                                                coroutineScope.launch {
                                                    // Mantém os limites ao atualizar apenas a disponibilidade
                                                    val req = ItemCardapioRequest(
                                                        produto.nome,
                                                        produto.categoria,
                                                        produto.preco,
                                                        novoStatus,
                                                        produto.parceria,
                                                        produto.limiteDiario,
                                                        produto.estoqueAtual
                                                    )
                                                    RotisseriaApi.atualizarPrato(produto.id, req)
                                                    carregarCardapio()
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(corDivisor))
                    Spacer(modifier = Modifier.width(32.dp))

                    // --- COLUNA DIREITA: FORMULÁRIO FIXO ---
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Card(colors = CardDefaults.cardColors(containerColor = corFundoCard), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {

                            // 👇 MÁGICA DO SCROLL AQUI 👇
                            Column(
                                modifier = Modifier
                                    .padding(32.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = if (pratoEmEdicao == null) "ADICIONAR NOVO PRATO" else "EDITANDO: ${pratoEmEdicao!!.nome}",
                                    color = corTextoDestaque, fontSize = 20.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = nome, onValueChange = { nome = it }, label = { Text("Nome do Prato", color = corTextoClaro, fontFamily = FidalgaFont) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque, cursorColor = corTextoDestaque), modifier = Modifier.fillMaxWidth()
                                )

                                ExposedDropdownMenuBox(
                                    expanded = menuExpandido,
                                    onExpandedChange = { menuExpandido = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = categoria,
                                        onValueChange = { categoria = it; menuExpandido = true },
                                        label = { Text("Categoria (Escolha ou Digite Nova)", color = corTextoClaro, fontFamily = FidalgaFont) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpandido) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque, cursorColor = corTextoDestaque),
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    if (categoriasExistentes.isNotEmpty()) {
                                        ExposedDropdownMenu(
                                            expanded = menuExpandido, onDismissRequest = { menuExpandido = false },
                                            modifier = Modifier.background(corFundoApp)
                                        ) {
                                            categoriasExistentes.forEach { opcao ->
                                                DropdownMenuItem(
                                                    text = { Text(opcao, color = corTextoClaro, fontFamily = FidalgaFont) },
                                                    onClick = { categoria = opcao; menuExpandido = false }
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = precoStr, onValueChange = { precoStr = it }, label = { Text("Preço (R$)", color = corTextoClaro, fontFamily = FidalgaFont) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque, cursorColor = corTextoDestaque), modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = parceria, onValueChange = { parceria = it }, label = { Text("Parceria (ou 'Nenhuma')", color = corTextoClaro, fontFamily = FidalgaFont) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque, cursorColor = corTextoDestaque), modifier = Modifier.fillMaxWidth()
                                )

                                // --- NOVA UI DO LIMITE DIÁRIO (FASE 2) ---
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Switch(
                                        checked = temLimiteDiario,
                                        onCheckedChange = { temLimiteDiario = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = corFundoCard, checkedTrackColor = corTextoDestaque, uncheckedThumbColor = corTextoClaro, uncheckedTrackColor = corFundoApp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tem limite de estoque hoje?", color = corTextoClaro, fontFamily = FidalgaFont)
                                }

                                if (temLimiteDiario) {
                                    OutlinedTextField(
                                        value = limiteDiarioInput,
                                        onValueChange = { texto -> limiteDiarioInput = texto.filter { it.isDigit() } },
                                        label = { Text("Quantidade Preparada (Ex: 30)", color = corTextoClaro, fontFamily = FidalgaFont) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro, focusedBorderColor = corTextoDestaque, cursorColor = corTextoDestaque), modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    if (pratoEmEdicao != null) {
                                        TextButton(onClick = {
                                            pratoEmEdicao = null
                                            nome = ""
                                            categoria = ""
                                            precoStr = ""
                                            parceria = "Nenhuma"
                                            temLimiteDiario = false
                                            limiteDiarioInput = ""
                                        }, modifier = Modifier.weight(1f)) {
                                            Text("CANCELAR", color = corTextoClaro, fontFamily = FidalgaFont)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val preco = precoStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                            if (nome.isNotBlank() && categoria.isNotBlank()) {
                                                coroutineScope.launch {
                                                    val statusAtual = pratoEmEdicao?.disponivel ?: true

                                                    // Montando o Request com a lógica do Estoque
                                                    val limiteInt = if (temLimiteDiario && limiteDiarioInput.isNotEmpty()) limiteDiarioInput.toInt() else null
                                                    val request = ItemCardapioRequest(
                                                        nome = nome,
                                                        categoria = categoria,
                                                        preco = preco,
                                                        disponivel = statusAtual,
                                                        parceria = parceria,
                                                        limiteDiario = limiteInt,
                                                        estoqueAtual = limiteInt // O estoque inicial nasce igual ao limite definido
                                                    )

                                                    val sucesso = if (pratoEmEdicao == null) {
                                                        RotisseriaApi.adicionarPrato(request)
                                                    } else {
                                                        RotisseriaApi.atualizarPrato(pratoEmEdicao!!.id, request)
                                                    }

                                                    if (sucesso) {
                                                        pratoEmEdicao = null
                                                        nome = ""
                                                        categoria = ""
                                                        precoStr = ""
                                                        parceria = "Nenhuma"
                                                        temLimiteDiario = false
                                                        limiteDiarioInput = ""
                                                        carregarCardapio()
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = corTextoDestaque, contentColor = Color(0xFF362511)),
                                        modifier = Modifier.weight(if (pratoEmEdicao == null) 1f else 1.5f).height(50.dp)
                                    ) { Text("SALVAR PRATO", fontFamily = FidalgaFont, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardProdutoItem(produto: ItemCardapioResponse, corFundo: Color, corDestaque: Color, corClara: Color, isSelecionado: Boolean, onEditClick: () -> Unit, onToggleChange: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelecionado) corDestaque.copy(alpha = 0.1f) else corFundo),
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = produto.nome, color = if (produto.disponivel) corClara else corClara.copy(alpha = 0.5f), fontSize = 20.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "R$ ${"%.2f".format(produto.preco)}", color = corDestaque, fontSize = 18.sp, fontFamily = FidalgaFont)

                    // Mostra a tag de escassez na lista do Admin também (visualização rápida)
                    if (produto.estoqueAtual != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(text = "Restam ${produto.estoqueAtual}", color = Color(0xFFFF6B6B), fontSize = 12.sp, fontFamily = FidalgaFont)
                        }
                    }

                    if (produto.parceria != "Nenhuma" && produto.parceria.isNotBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.background(corDestaque.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(text = "🤝 ${produto.parceria}", color = corDestaque, fontSize = 12.sp, fontFamily = FidalgaFont)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Disponível", color = corClara.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FidalgaFont)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = produto.disponivel, onCheckedChange = { onToggleChange(it) }, colors = SwitchDefaults.colors(checkedThumbColor = corFundo, checkedTrackColor = Color(0xFF388E3C), uncheckedThumbColor = corClara, uncheckedTrackColor = corFundo.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = onEditClick, modifier = Modifier.background(corDestaque, RoundedCornerShape(8.dp)).size(40.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = corFundo)
                }
            }
        }
    }
}