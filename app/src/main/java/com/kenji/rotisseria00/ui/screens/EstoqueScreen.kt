package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseria00.models.ItemEstoqueRequest
import com.kenji.rotisseria00.models.ItemEstoqueResponse
import com.kenji.rotisseria00.network.RotisseriaApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EstoqueScreen() {
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)
    val corFundoCard = Color(0xFF362511)
    val corDivisor = Color(0xFF5A4A32)

    val coroutineScope = rememberCoroutineScope()

    // Estados Reais da Tela (Internet)
    var listaEstoque by remember { mutableStateOf<List<ItemEstoqueResponse>>(emptyList()) }
    var itemSelecionado by remember { mutableStateOf<ItemEstoqueResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Controle do Pop-up (Modal)
    var mostrarModal by remember { mutableStateOf(false) }
    var itemEmEdicao by remember { mutableStateOf<ItemEstoqueResponse?>(null) }

    // Função para buscar dados do Ktor
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

    // Carrega os dados assim que a tela abre
    LaunchedEffect(Unit) {
        carregarEstoque()
    }

    Scaffold(
        containerColor = corFundoApp,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    itemEmEdicao = null // Null significa "Criar Novo"
                    mostrarModal = true
                },
                containerColor = corTextoDestaque,
                contentColor = corFundoCard,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Item", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp)
        ) {
            Text("GESTÃO DE ESTOQUE", color = corTextoDestaque, fontSize = 32.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = corTextoDestaque)
                }
            } else if (listaEstoque.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Estoque vazio. Clique no + para adicionar.", color = corTextoClaro.copy(alpha = 0.5f), fontSize = 20.sp, fontFamily = FidalgaFont)
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // COLUNA ESQUERDA: LISTA
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ITENS EM ESTOQUE", color = corTextoClaro, fontSize = 20.sp, fontFamily = FidalgaFont)
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(listaEstoque) { item ->
                                val isSelecionado = itemSelecionado?.id == item.id
                                CardItemEstoque(item, isSelecionado, corTextoDestaque, corFundoCard, corTextoClaro,
                                    onClick = { itemSelecionado = item },
                                    onEditClick = {
                                        itemEmEdicao = item // Passa o item para o Modal editar
                                        mostrarModal = true
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(corDivisor))
                    Spacer(modifier = Modifier.width(32.dp))

                    // COLUNA DIREITA: DETALHES
                    Column(modifier = Modifier.weight(1.2f)) {
                        itemSelecionado?.let { item ->
                            CardDetalhesEstoque(item, corFundoCard, corTextoDestaque, corTextoClaro) {
                                // O botão de Ajustar Estoque também abre o modal de edição
                                itemEmEdicao = item
                                mostrarModal = true
                            }
                        }
                    }
                }
            }
        }
    }

    // --- O POP-UP (MODAL) DE CADASTRO/EDIÇÃO ---
    if (mostrarModal) {
        ModalEstoque(
            item = itemEmEdicao,
            corFundo = corFundoCard,
            corDestaque = corTextoDestaque,
            corTexto = corTextoClaro,
            onDismiss = { mostrarModal = false },
            onSave = { nome, atual, minima, unidade ->
                coroutineScope.launch {
                    val dataHoje = SimpleDateFormat("dd 'de' MMM 'de' yyyy", Locale("pt", "BR")).format(Date())
                    val request = ItemEstoqueRequest(nome, atual, minima, unidade, dataHoje.uppercase())

                    val sucesso = if (itemEmEdicao == null) {
                        RotisseriaApi.adicionarEstoque(request)
                    } else {
                        RotisseriaApi.atualizarEstoque(itemEmEdicao!!.id, request)
                    }

                    if (sucesso) {
                        mostrarModal = false
                        carregarEstoque() // Recarrega a lista para mostrar a mudança
                    }
                }
            }
        )
    }
}

// --- MODAL DE FORMULÁRIO ---
@Composable
fun ModalEstoque(
    item: ItemEstoqueResponse?,
    corFundo: Color,
    corDestaque: Color,
    corTexto: Color,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, String) -> Unit
) {
    var nome by remember { mutableStateOf(item?.nome ?: "") }
    var qtdAtual by remember { mutableStateOf(item?.quantidadeAtual?.toString() ?: "") }
    var qtdMinima by remember { mutableStateOf(item?.quantidadeMinima?.toString() ?: "") }
    var unidade by remember { mutableStateOf(item?.unidade ?: "KG") } // Padrão é KG

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = corFundo,
        title = {
            Text(if (item == null) "NOVO ITEM" else "EDITAR ITEM", color = corDestaque, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it.uppercase() },
                    label = { Text("Nome do Produto (Ex: QUEIJO MUSSARELA)", color = corTexto) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTexto, unfocusedTextColor = corTexto, focusedBorderColor = corDestaque, cursorColor = corDestaque),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtdAtual, onValueChange = { qtdAtual = it },
                        label = { Text("Qtd Atual", color = corTexto) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTexto, unfocusedTextColor = corTexto, focusedBorderColor = corDestaque, cursorColor = corDestaque),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unidade, onValueChange = { unidade = it.uppercase() },
                        label = { Text("UN, KG, L", color = corTexto) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTexto, unfocusedTextColor = corTexto, focusedBorderColor = corDestaque, cursorColor = corDestaque),
                        modifier = Modifier.weight(0.8f)
                    )
                }
                OutlinedTextField(
                    value = qtdMinima, onValueChange = { qtdMinima = it },
                    label = { Text("Alerta de Qtd Mínima", color = corTexto) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = corTexto, unfocusedTextColor = corTexto, focusedBorderColor = corDestaque, cursorColor = corDestaque),
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
                colors = ButtonDefaults.buttonColors(containerColor = corDestaque, contentColor = Color(0xFF362511))
            ) { Text("SALVAR", fontFamily = FidalgaFont, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = corTexto, fontFamily = FidalgaFont) }
        }
    )
}

// --- CARTÃO DA LISTA (Atualizado para o DTO) ---
@Composable
fun CardItemEstoque(item: ItemEstoqueResponse, isSelecionado: Boolean, corDestaque: Color, corFundoCard: Color, corClara: Color, onClick: () -> Unit, onEditClick: () -> Unit) {
    val backgroundColor = if (isSelecionado) corDestaque else corFundoCard
    val textColor = if (isSelecionado) Color(0xFF362511) else corClara

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${item.nome} - ${item.quantidadeAtual}${item.unidade}",
                    color = textColor, fontSize = 18.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(MIN: ${item.quantidadeMinima}${item.unidade})",
                    color = textColor.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = FidalgaFont
                )
            }
            if (isSelecionado) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF362511))
                }
            }
        }
    }
}

// --- CARTÃO DE DETALHES (Atualizado para o DTO) ---
@Composable
fun CardDetalhesEstoque(item: ItemEstoqueResponse, corFundoCard: Color, corDestaque: Color, corClara: Color, onAjustarClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = corFundoCard),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text("DETALHES DO ITEM: ${item.nome}", color = corClara, fontSize = 20.sp, fontFamily = FidalgaFont)

            // O Box da imagem foi removido daqui!
            // Aumentamos o espaçamento para o cartão respirar melhor
            Spacer(modifier = Modifier.height(40.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("QUANTIDADE:", color = corClara.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = FidalgaFont)
                    Text("${item.quantidadeAtual}${item.unidade}", color = corClara, fontSize = 28.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("QUANTIDADE MÍNIMA:", color = corClara.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = FidalgaFont)
                    Text("${item.quantidadeMinima}${item.unidade}", color = corClara, fontSize = 28.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("ÚLTIMA DATA DE COMPRA:", color = corClara.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = FidalgaFont)
            Text(item.ultimaCompra, color = corClara, fontSize = 20.sp, fontFamily = FidalgaFont)

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onAjustarClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = corDestaque, contentColor = Color(0xFF362511))
            ) { Text("AJUSTAR ESTOQUE", fontSize = 18.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold) }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { /* Lógica futura de histórico */ }, modifier = Modifier.fillMaxWidth()) {
                Text("VER HISTÓRICO", color = corClara, fontSize = 16.sp, fontFamily = FidalgaFont)
            }
        }
    }
}