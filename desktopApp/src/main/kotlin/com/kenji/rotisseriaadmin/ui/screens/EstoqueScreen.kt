package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseriaadmin.data.ItemEstoqueRequest
import com.kenji.rotisseriaadmin.data.ItemEstoqueResponse
import com.kenji.rotisseriaadmin.data.RotisseriaApi
import com.kenji.rotisseriaadmin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EstoqueScreen() {
    val coroutineScope = rememberCoroutineScope()

    var listaEstoque by remember { mutableStateOf<List<ItemEstoqueResponse>>(emptyList()) }
    var itemSelecionado by remember { mutableStateOf<ItemEstoqueResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var mostrarModal by remember { mutableStateOf(false) }
    var itemEmEdicao by remember { mutableStateOf<ItemEstoqueResponse?>(null) }

    fun carregarEstoque() {
        coroutineScope.launch {
            isLoading = true
            listaEstoque = RotisseriaApi.buscarEstoque()
            if (listaEstoque.isNotEmpty() && itemSelecionado == null) {
                itemSelecionado = listaEstoque.first()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        carregarEstoque()
    }

    Scaffold(
        backgroundColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    itemEmEdicao = null
                    mostrarModal = true
                },
                backgroundColor = SecondaryOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Item", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp) // Adicionado padding global da tela
        ) {
            Text("Gestão de Estoque", color = TextDarkBrown, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBrown)
                }
            } else if (listaEstoque.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Estoque vazio. Clique no + para adicionar.", color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // =====================================
                    // COLUNA ESQUERDA: LISTA COM SCROLLBAR
                    // =====================================
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text("ITENS CADASTRADOS", color = PrimaryBrown, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(16.dp))

                        val state = rememberLazyListState()

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = state,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize().padding(end = 16.dp)
                            ) {
                                items(listaEstoque) { item ->
                                    val isSelecionado = itemSelecionado?.id == item.id
                                    CardItemEstoque(
                                        item = item,
                                        isSelecionado = isSelecionado,
                                        onClick = { itemSelecionado = item },
                                        onEditClick = {
                                            itemEmEdicao = item
                                            mostrarModal = true
                                        }
                                    )
                                }
                            }
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(scrollState = state)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(TextDarkBrown.copy(alpha = 0.1f)))
                    Spacer(modifier = Modifier.width(32.dp))

                    // =====================================
                    // COLUNA DIREITA: DETALHES
                    // =====================================
                    Column(modifier = Modifier.weight(1f)) {
                        itemSelecionado?.let { item ->
                            CardDetalhesEstoque(item) {
                                itemEmEdicao = item
                                mostrarModal = true
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarModal) {
        ModalEstoque(
            item = itemEmEdicao,
            onDismiss = { mostrarModal = false },
            onSave = { nome, atual, minima, unidade ->
                coroutineScope.launch {
                    val dataHoje = SimpleDateFormat("dd 'de' MMM 'de' yyyy", Locale("pt", "BR")).format(Date())
                    val request = ItemEstoqueRequest(nome, atual, minima, unidade, dataHoje.uppercase())

                    val sucesso = if (itemEmEdicao == null) RotisseriaApi.adicionarEstoque(request)
                    else RotisseriaApi.atualizarEstoque(itemEmEdicao!!.id, request)

                    if (sucesso) {
                        mostrarModal = false
                        carregarEstoque()
                    }
                }
            }
        )
    }
}

@Composable
fun CardItemEstoque(item: ItemEstoqueResponse, isSelecionado: Boolean, onClick: () -> Unit, onEditClick: () -> Unit) {
    val backgroundColor = if (isSelecionado) PrimaryBrown else SurfaceWhite
    val textColor = if (isSelecionado) OnPrimary else TextDarkBrown
    val emAlerta = item.quantidadeAtual <= item.quantidadeMinima

    Card(
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        elevation = if (isSelecionado) 8.dp else 2.dp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (emAlerta) {
                        Icon(Icons.Default.Warning, contentDescription = "Alerta", tint = if (isSelecionado) SecondaryOrange else CorAlerta, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = item.nome,
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Qtd: ${item.quantidadeAtual}${item.unidade}", color = if (emAlerta && !isSelecionado) CorAlerta else textColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = "  |  Mínimo: ${item.quantidadeMinima}${item.unidade}", color = if (isSelecionado) textColor.copy(alpha = 0.8f) else TextDarkBrown, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (isSelecionado) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.background(SecondaryOrange, RoundedCornerShape(8.dp)).size(48.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun CardDetalhesEstoque(item: ItemEstoqueResponse, onAjustarClick: () -> Unit) {
    val emAlerta = item.quantidadeAtual <= item.quantidadeMinima

    Card(backgroundColor = SurfaceWhite, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text("DETALHES DO PRODUTO", color = PrimaryBrown, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.nome, color = TextDarkBrown, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

            Divider(modifier = Modifier.padding(vertical = 24.dp), color = TextDarkBrown.copy(alpha = 0.1f), thickness = 2.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("EM ESTOQUE", color = TextDarkBrown, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${item.quantidadeAtual}${item.unidade}", color = if (emAlerta) CorAlerta else TextDarkBrown, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ALERTA MÍNIMO", color = TextDarkBrown, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${item.quantidadeMinima}${item.unidade}", color = TextDarkBrown, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.fillMaxWidth().background(BackgroundCream, RoundedCornerShape(8.dp)).padding(16.dp)) {
                Column {
                    Text("ÚLTIMA ATUALIZAÇÃO / COMPRA:", color = TextDarkBrown, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.ultimaCompra, color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onAjustarClick,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBrown, contentColor = OnPrimary)
            ) {
                Text("AJUSTAR ESTOQUE", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { /* Histórico futuro */ }, modifier = Modifier.fillMaxWidth()) {
                Text("VER HISTÓRICO", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ModalEstoque(
    item: ItemEstoqueResponse?,
    onDismiss: () -> Unit, onSave: (String, Double, Double, String) -> Unit
) {
    var nome by remember { mutableStateOf(item?.nome ?: "") }
    var qtdAtual by remember { mutableStateOf(item?.quantidadeAtual?.toString() ?: "") }
    var qtdMinima by remember { mutableStateOf(item?.quantidadeMinima?.toString() ?: "") }
    var unidade by remember { mutableStateOf(item?.unidade ?: "KG") }

    val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = TextDarkBrown,
        focusedBorderColor = PrimaryBrown,
        cursorColor = PrimaryBrown,
        unfocusedBorderColor = TextDarkBrown.copy(alpha = 0.5f)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (item == null) "NOVO ITEM" else "EDITAR ITEM",
                color = SecondaryOrange,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it.uppercase() },
                    label = { Text("Nome do Produto", fontWeight = FontWeight.Medium) },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = qtdAtual, onValueChange = { qtdAtual = it },
                        label = { Text("Qtd Atual", fontWeight = FontWeight.Medium) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unidade, onValueChange = { unidade = it.uppercase() },
                        label = { Text("UN, KG, L", fontWeight = FontWeight.Medium) },
                        colors = textFieldColors,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                OutlinedTextField(
                    value = qtdMinima, onValueChange = { qtdMinima = it },
                    label = { Text("Alerta Mínimo", fontWeight = FontWeight.Medium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val atual = qtdAtual.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val min = qtdMinima.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (nome.isNotBlank()) onSave(nome, atual, min, unidade)
                },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBrown, contentColor = OnPrimary)
            ) { Text("SALVAR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("CANCELAR", color = TextDarkBrown, fontWeight = FontWeight.Bold)
            }
        }
    )
}