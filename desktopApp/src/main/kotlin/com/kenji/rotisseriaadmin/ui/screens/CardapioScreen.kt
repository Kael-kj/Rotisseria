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

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (isLoading) {
            CircularProgressIndicator(color = PrimaryBrown, modifier = Modifier.align(Alignment.Center))
        } else {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                // COLUNA ESQUERDA: LISTA DO CARDÁPIO
                Column(modifier = Modifier.weight(1.3f).fillMaxHeight()) {
                    Text("Gestão de Cardápio", color = TextDarkBrown, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        cardapio.groupBy { it.categoria }.forEach { (categoria, pratos) ->
                            item { Text(categoria.uppercase(), color = PrimaryBrown, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                            items(pratos) { prato ->
                                Card(backgroundColor = SurfaceWhite, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prato.nome, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDarkBrown)
                                            Text("R$ ${"%.2f".format(prato.preco)}", color = TextDarkBrown.copy(alpha = 0.5f))
                                        }

                                        // SWITCH DE DISPONIBILIDADE (O QUE VOCÊ PEDIU)
                                        Switch(
                                            checked = prato.disponivel,
                                            onCheckedChange = { novoStatus ->
                                                // 1. Muda na tela instantaneamente para não parecer travado
                                                cardapio = cardapio.map { if (it.id == prato.id) it.copy(disponivel = novoStatus) else it }

                                                // 2. Avisa o servidor em segundo plano
                                                coroutineScope.launch {
                                                    val request = ItemCardapioRequest(
                                                        prato.nome, prato.categoria, prato.preco,
                                                        novoStatus, // Novo valor aqui
                                                        prato.parceria, prato.limiteDiario, prato.estoqueAtual, prato.ingredientes
                                                    )
                                                    RotisseriaApi.atualizarPrato(prato.id, request)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = CorVerde)
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        IconButton(onClick = { editarPrato(prato) }, modifier = Modifier.background(SecondaryOrange, RoundedCornerShape(8.dp)).size(40.dp)) {
                                            Icon(Icons.Default.Edit, null, tint = SurfaceWhite, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // COLUNA DIREITA: FORMULÁRIO COMPLETO
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // BOTÃO NOVO PRATO (MUITO IMPORTANTE!)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (pratoEmEdicao == null) "ADICIONAR NOVO" else "EDITANDO PRATO", color = SecondaryOrange, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { limparFormulario() },
                            colors = ButtonDefaults.buttonColors(backgroundColor = SecondaryOrange, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Text(" NOVO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(value = formNome, onValueChange = { formNome = it }, label = { Text("Nome do Prato") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    // Categorias
                    Box {
                        OutlinedTextField(value = formCategoria, onValueChange = { formCategoria = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { dropdownCategoriaExpanded = true }) })
                        DropdownMenu(expanded = dropdownCategoriaExpanded, onDismissRequest = { dropdownCategoriaExpanded = false }) {
                            listOf("Prato Principal", "Acompanhamentos", "Bebidas", "Sobremesas", "Doce").forEach { cat ->
                                DropdownMenuItem(onClick = { formCategoria = cat; dropdownCategoriaExpanded = false }) { Text(cat) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = formPreco, onValueChange = { formPreco = it }, label = { Text("Preço (R$)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = formParceria, onValueChange = { formParceria = it }, label = { Text("Parceria") }, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // LIMITES
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = formTemLimite, onCheckedChange = { formTemLimite = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryBrown))
                        Text(" Tem limite diário?", fontSize = 14.sp)
                        if (formTemLimite) {
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedTextField(value = formLimiteDiario, onValueChange = { formLimiteDiario = it }, label = { Text("Qtd") }, modifier = Modifier.width(80.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = SurfaceWhite, thickness = 2.dp)

                    // FICHA TÉCNICA
                    Text("FICHA TÉCNICA (Baixa estoque)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(2f)) {
                            OutlinedTextField(value = ingredienteSelecionado?.nome ?: "Ingrediente", onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Icon(Icons.Default.Search, null, Modifier.clickable { dropdownEstoqueExpanded = true }) })
                            DropdownMenu(expanded = dropdownEstoqueExpanded, onDismissRequest = { dropdownEstoqueExpanded = false }) {
                                estoque.forEach { item -> DropdownMenuItem(onClick = { ingredienteSelecionado = item; dropdownEstoqueExpanded = false }) { Text(item.nome) } }
                            }
                        }
                        OutlinedTextField(value = ingredienteQtd, onValueChange = { ingredienteQtd = it }, label = { Text("Qtd") }, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            if (ingredienteSelecionado != null && ingredienteQtd.isNotEmpty()) {
                                formIngredientes = formIngredientes + IngredienteNoPrato(ingredienteSelecionado!!.nome, ingredienteQtd.replace(",",".").toDouble())
                                ingredienteSelecionado = null; ingredienteQtd = ""
                            }
                        }, modifier = Modifier.background(SecondaryOrange, RoundedCornerShape(8.dp))) { Icon(Icons.Default.Add, null, tint = SurfaceWhite) }
                    }

                    LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                        items(formIngredientes) { ing ->
                            Row(modifier = Modifier.fillMaxWidth().background(SurfaceWhite.copy(0.5f), RoundedCornerShape(4.dp)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${ing.quantidadeNecessaria}x ${ing.nomeIngrediente}", fontSize = 12.sp)
                                Icon(Icons.Default.Close, null, Modifier.size(16.dp).clickable { formIngredientes = formIngredientes - ing }, tint = CorAlerta)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // O BOTÃO DE SALVAR (CADASTRAR OU ATUALIZAR)
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val limit = if(formTemLimite) formLimiteDiario.toIntOrNull() else null

                                // ==========================================
                                // A MATEMÁTICA INTELIGENTE DO ESTOQUE
                                // ==========================================
                                val estoqueParaSalvar = if (pratoEmEdicao == null) {
                                    limit // Prato novo: o estoque começa igual ao limite digitado
                                } else {
                                    if (limit != null) {
                                        // Prato editado: Calcula quantos itens você adicionou ou removeu
                                        val limiteAntigo = pratoEmEdicao!!.limiteDiario ?: 0
                                        val diferenca = limit - limiteAntigo

                                        // Soma essa diferença no estoque atual que sobrou na panela
                                        val novoEstoque = (pratoEmEdicao!!.estoqueAtual ?: 0) + diferenca

                                        // Proteção para o estoque não ficar negativo se você errar a digitação
                                        if (novoEstoque < 0) 0 else novoEstoque
                                    } else {
                                        null // Se você desmarcou a caixinha de limite, libera o estoque infinito
                                    }
                                }

                                val request = ItemCardapioRequest(
                                    nome = formNome,
                                    categoria = formCategoria,
                                    preco = formPreco.replace(",",".").toDoubleOrNull() ?: 0.0,
                                    disponivel = pratoEmEdicao?.disponivel ?: true,
                                    parceria = formParceria,
                                    limiteDiario = limit,
                                    estoqueAtual = estoqueParaSalvar, // Agora salva o valor calculado!
                                    ingredientes = formIngredientes
                                )

                                val sucesso = if (pratoEmEdicao == null) RotisseriaApi.adicionarPrato(request)
                                else RotisseriaApi.atualizarPrato(pratoEmEdicao!!.id, request)

                                if (sucesso) { carregarDados(); limparFormulario() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBrown, contentColor = OnPrimary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = formNome.isNotBlank() && formPreco.isNotBlank()
                    ) {
                        Text(if (pratoEmEdicao == null) "CADASTRAR NO CARDÁPIO" else "SALVAR ALTERAÇÕES", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}