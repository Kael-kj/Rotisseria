package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseriaadmin.data.*
import com.kenji.rotisseriaadmin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CardapioScreen() {
    val coroutineScope = rememberCoroutineScope()

    // ESTADOS DE DADOS
    var cardapio by remember { mutableStateOf<List<ItemCardapioResponse>>(emptyList()) }
    var estoque by remember { mutableStateOf<List<ItemEstoqueResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // ESTADOS DO FORMULÁRIO
    var pratoEmEdicao by remember { mutableStateOf<ItemCardapioResponse?>(null) }
    var formNome by remember { mutableStateOf("") }
    var formCategoria by remember { mutableStateOf("") }
    var formPreco by remember { mutableStateOf("") }
    var formParceria by remember { mutableStateOf("Nenhuma") }
    var formTemLimite by remember { mutableStateOf(false) }
    var formLimiteDiario by remember { mutableStateOf("") }
    var formIngredientes by remember { mutableStateOf<List<IngredienteNoPrato>>(emptyList()) }

    // Auxiliares
    var ingredienteSelecionado by remember { mutableStateOf<ItemEstoqueResponse?>(null) }
    var ingredienteQtd by remember { mutableStateOf("") }
    var dropdownCategoriaExpanded by remember { mutableStateOf(false) }
    var dropdownEstoqueExpanded by remember { mutableStateOf(false) }

    fun carregarDados() {
        coroutineScope.launch {
            isLoading = true
            try {
                cardapio = RotisseriaApi.buscarCardapio()
                estoque = RotisseriaApi.buscarEstoque()
            } catch (e: Exception) { e.printStackTrace() }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { carregarDados() }

    fun limparFormulario() {
        pratoEmEdicao = null
        formNome = ""
        formCategoria = ""
        formPreco = ""
        formParceria = "Nenhuma"
        formTemLimite = false
        formLimiteDiario = ""
        formIngredientes = emptyList()
    }

    fun editarPrato(prato: ItemCardapioResponse) {
        pratoEmEdicao = prato
        formNome = prato.nome
        formCategoria = prato.categoria
        formPreco = prato.preco.toString()
        formParceria = prato.parceria
        formTemLimite = prato.limiteDiario != null
        formLimiteDiario = prato.limiteDiario?.toString() ?: ""
        formIngredientes = prato.ingredientes
    }

    // Padrão de cores para os TextFields para reaproveitamento
    val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        textColor = TextDarkBrown,
        focusedBorderColor = PrimaryBrown,
        cursorColor = PrimaryBrown,
        unfocusedBorderColor = TextDarkBrown.copy(alpha = 0.5f)
    )

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (isLoading) {
            CircularProgressIndicator(color = PrimaryBrown, modifier = Modifier.align(Alignment.Center))
        } else {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                // ==========================================
                // COLUNA ESQUERDA: LISTA DO CARDÁPIO
                // ==========================================
                Column(modifier = Modifier.weight(1.3f).fillMaxHeight()) {
                    Text("Gestão de Cardápio", color = TextDarkBrown, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(24.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        cardapio.groupBy { it.categoria }.forEach { (categoria, pratos) ->
                            item {
                                Text(
                                    text = categoria.uppercase(),
                                    color = PrimaryBrown,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                                )
                            }
                            items(pratos) { prato ->
                                Card(
                                    backgroundColor = SurfaceWhite,
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prato.nome, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDarkBrown)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("R$ ${"%.2f".format(prato.preco)}", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        }

                                        Switch(
                                            checked = prato.disponivel,
                                            onCheckedChange = { novoStatus ->
                                                cardapio = cardapio.map { if (it.id == prato.id) it.copy(disponivel = novoStatus) else it }
                                                coroutineScope.launch {
                                                    val request = ItemCardapioRequest(
                                                        prato.nome, prato.categoria, prato.preco,
                                                        novoStatus,
                                                        prato.parceria, prato.limiteDiario, prato.estoqueAtual, prato.ingredientes
                                                    )
                                                    RotisseriaApi.atualizarPrato(prato.id, request)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CorVerde)
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        IconButton(
                                            onClick = { editarPrato(prato) },
                                            modifier = Modifier.background(PrimaryBrown, RoundedCornerShape(8.dp)).size(48.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = SurfaceWhite, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // COLUNA DIREITA: FORMULÁRIO COMPLETO
                // ==========================================
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (pratoEmEdicao == null) "NOVO PRATO" else "EDITANDO PRATO",
                            color = SecondaryOrange,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Button(
                            onClick = { limparFormulario() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = SecondaryOrange, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Novo", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("NOVO", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = formNome,
                        onValueChange = { formNome = it },
                        label = { Text("Nome do Prato", fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box {
                        OutlinedTextField(
                            value = formCategoria,
                            onValueChange = { formCategoria = it },
                            label = { Text("Categoria", fontWeight = FontWeight.Medium) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = textFieldColors,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir", Modifier.clickable { dropdownCategoriaExpanded = true }, tint = TextDarkBrown) }
                        )
                        DropdownMenu(expanded = dropdownCategoriaExpanded, onDismissRequest = { dropdownCategoriaExpanded = false }) {
                            listOf("Prato Principal", "Acompanhamentos", "Bebidas", "Sobremesas", "Doce").forEach { cat ->
                                DropdownMenuItem(onClick = { formCategoria = cat; dropdownCategoriaExpanded = false }) {
                                    Text(cat, color = TextDarkBrown, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = formPreco,
                            onValueChange = { formPreco = it },
                            label = { Text("Preço (R$)", fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = textFieldColors
                        )
                        OutlinedTextField(
                            value = formParceria,
                            onValueChange = { formParceria = it },
                            label = { Text("Parceria", fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // LIMITES
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(BackgroundCream, RoundedCornerShape(8.dp)).padding(8.dp).fillMaxWidth()) {
                        Checkbox(
                            checked = formTemLimite,
                            onCheckedChange = { formTemLimite = it },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBrown, uncheckedColor = TextDarkBrown)
                        )
                        Text("Tem limite diário?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDarkBrown)
                        if (formTemLimite) {
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedTextField(
                                value = formLimiteDiario,
                                onValueChange = { formLimiteDiario = it },
                                label = { Text("Qtd") },
                                modifier = Modifier.width(100.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = textFieldColors
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 24.dp), color = TextDarkBrown.copy(alpha = 0.1f), thickness = 2.dp)

                    // FICHA TÉCNICA
                    Text("FICHA TÉCNICA (Baixa estoque)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextDarkBrown)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(2f)) {
                            OutlinedTextField(
                                value = ingredienteSelecionado?.nome ?: "Selecionar Ingrediente",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", Modifier.clickable { dropdownEstoqueExpanded = true }, tint = TextDarkBrown) }
                            )
                            DropdownMenu(expanded = dropdownEstoqueExpanded, onDismissRequest = { dropdownEstoqueExpanded = false }) {
                                estoque.forEach { item ->
                                    DropdownMenuItem(onClick = { ingredienteSelecionado = item; dropdownEstoqueExpanded = false }) {
                                        Text(item.nome, color = TextDarkBrown, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = ingredienteQtd,
                            onValueChange = { ingredienteQtd = it },
                            label = { Text("Qtd") },
                            modifier = Modifier.weight(1f),
                            colors = textFieldColors
                        )
                        IconButton(
                            onClick = {
                                if (ingredienteSelecionado != null && ingredienteQtd.isNotEmpty()) {
                                    formIngredientes = formIngredientes + IngredienteNoPrato(ingredienteSelecionado!!.nome, ingredienteQtd.replace(",",".").toDouble())
                                    ingredienteSelecionado = null; ingredienteQtd = ""
                                }
                            },
                            modifier = Modifier.background(SecondaryOrange, RoundedCornerShape(8.dp)).size(56.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = SurfaceWhite, modifier = Modifier.size(28.dp))
                        }
                    }

                    LazyColumn(modifier = Modifier.weight(1f).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(formIngredientes) { ing ->
                            Row(
                                modifier = Modifier.fillMaxWidth().background(BackgroundCream, RoundedCornerShape(8.dp)).padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${ing.quantidadeNecessaria}x  ${ing.nomeIngrediente}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDarkBrown)
                                Icon(Icons.Default.Close, contentDescription = "Remover", Modifier.size(24.dp).clickable { formIngredientes = formIngredientes - ing }, tint = CorAlerta)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val limit = if(formTemLimite) formLimiteDiario.toIntOrNull() else null

                                val estoqueParaSalvar = if (pratoEmEdicao == null) {
                                    limit
                                } else {
                                    if (limit != null) {
                                        val limiteAntigo = pratoEmEdicao!!.limiteDiario ?: 0
                                        val diferenca = limit - limiteAntigo
                                        val novoEstoque = (pratoEmEdicao!!.estoqueAtual ?: 0) + diferenca
                                        if (novoEstoque < 0) 0 else novoEstoque
                                    } else {
                                        null
                                    }
                                }

                                val request = ItemCardapioRequest(
                                    nome = formNome,
                                    categoria = formCategoria,
                                    preco = formPreco.replace(",",".").toDoubleOrNull() ?: 0.0,
                                    disponivel = pratoEmEdicao?.disponivel ?: true,
                                    parceria = formParceria,
                                    limiteDiario = limit,
                                    estoqueAtual = estoqueParaSalvar,
                                    ingredientes = formIngredientes
                                )

                                val sucesso = if (pratoEmEdicao == null) RotisseriaApi.adicionarPrato(request)
                                else RotisseriaApi.atualizarPrato(pratoEmEdicao!!.id, request)

                                if (sucesso) { carregarDados(); limparFormulario() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBrown, contentColor = OnPrimary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = formNome.isNotBlank() && formPreco.isNotBlank()
                    ) {
                        Text(
                            text = if (pratoEmEdicao == null) "CADASTRAR NO CARDÁPIO" else "SALVAR ALTERAÇÕES",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}