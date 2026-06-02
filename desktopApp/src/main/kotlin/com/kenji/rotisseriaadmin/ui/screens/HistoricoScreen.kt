package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Print
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.WeekFields
import java.util.*

@Composable
fun HistoricoScreen() {
    val coroutineScope = rememberCoroutineScope()

    // ==========================================
    // 1. ESTADOS PRINCIPAIS
    // ==========================================
    var historicoComandas by remember { mutableStateOf<List<ComandaResponse>>(emptyList()) }
    var comandaSelecionada by remember { mutableStateOf<ComandaResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados dos Filtros
    val filtrosTempo = listOf("HOJE", "ESTA SEMANA", "ESTE MÊS", "TUDO")
    var filtroTempoSelecionado by remember { mutableStateOf("TUDO") }
    var filtroPrato by remember { mutableStateOf("Todos") }
    var filtroCategoria by remember { mutableStateOf("Todas") }
    var filtroParceria by remember { mutableStateOf("Todas") }

    // Carregamento Real dos Dados
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            historicoComandas = RotisseriaApi.buscarHistorico()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoading = false
    }

    // ==========================================
    // 2. LÓGICA DE OPÇÕES DINÂMICAS PARA OS DROPDOWNS
    // ==========================================
    // Lê todas as comandas baixadas e extrai os itens únicos para não termos opções repetidas ou vazias
    val opcoesPratos = remember(historicoComandas) {
        listOf("Todos") + historicoComandas.flatMap { it.itens }.map { it.nome }.distinct().sorted()
    }
    val opcoesCategorias = remember(historicoComandas) {
        listOf("Todas") + historicoComandas.flatMap { it.itens }.map { it.categoria }.distinct().sorted()
    }
    val opcoesParcerias = remember(historicoComandas) {
        listOf("Todas") + historicoComandas.flatMap { it.itens }.map { it.parceria }.distinct().sorted()
    }

    // ==========================================
    // 3. O MOTOR DOS FILTROS (Reativo)
    // ==========================================
    val comandasFiltradas = remember(historicoComandas, filtroTempoSelecionado, filtroPrato, filtroCategoria, filtroParceria) {
        historicoComandas.filter { comanda ->
            // A. Filtro de Tempo
            val passaTempo = isDataDentroDoFiltro(comanda.dataFechamento, filtroTempoSelecionado)

            // B. Filtro de Itens (Se for "Todos", passa direto. Se não, procura dentro da comanda)
            val passaPrato = filtroPrato == "Todos" || comanda.itens.any { it.nome == filtroPrato }
            val passaCategoria = filtroCategoria == "Todas" || comanda.itens.any { it.categoria == filtroCategoria }
            val passaParceria = filtroParceria == "Todas" || comanda.itens.any { it.parceria == filtroParceria }

            // A comanda só aparece se passar em todos os testes simultaneamente
            passaTempo && passaPrato && passaCategoria && passaParceria
        }
    }

    // O total vendido agora calcula apenas o que está aparecendo na tela!
    val totalVendido = comandasFiltradas.sumOf { it.total }

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (isLoading) {
            CircularProgressIndicator(color = PrimaryBrown, modifier = Modifier.align(Alignment.Center))
        } else {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                // COLUNA DA ESQUERDA: FILTROS E LISTA
                Column(modifier = Modifier.weight(1.3f)) {
                    Text("Histórico de Vendas", color = TextDarkBrown, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(24.dp))

                    // LINHA 1: Filtros de Tempo (Chips)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        filtrosTempo.forEach { tempo ->
                            val isSelecionado = tempo == filtroTempoSelecionado
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelecionado) SecondaryOrange else SurfaceWhite)
                                    .clickable {
                                        filtroTempoSelecionado = tempo
                                        comandaSelecionada = null // Limpa a seleção ao trocar de filtro
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = tempo,
                                    color = if (isSelecionado) Color.White else TextDarkBrown,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // LINHA 2: Filtros Dropdown Interativos
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FiltroDropdown("Prato", filtroPrato, opcoesPratos, { filtroPrato = it; comandaSelecionada = null }, modifier = Modifier.weight(1f))
                        FiltroDropdown("Categoria", filtroCategoria, opcoesCategorias, { filtroCategoria = it; comandaSelecionada = null }, modifier = Modifier.weight(1f))
                        FiltroDropdown("Parceria", filtroParceria, opcoesParcerias, { filtroParceria = it; comandaSelecionada = null }, modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SecondaryOrange)
                                .clickable {
                                    // Botão de Reset rápido para limpar tudo de uma vez
                                    filtroTempoSelecionado = "TUDO"
                                    filtroPrato = "Todos"
                                    filtroCategoria = "Todas"
                                    filtroParceria = "Todas"
                                    comandaSelecionada = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Limpar Filtros", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // LINHA 3: Resumo Dinâmico
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Pedidos Filtrados (${comandasFiltradas.size})", color = TextDarkBrown, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("R$ ${"%.2f".format(totalVendido)}", color = SecondaryOrange, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // LISTA DE VENDAS (Agora usa comandasFiltradas)
                    if (comandasFiltradas.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("Nenhum pedido encontrado com esses filtros.", color = TextDarkBrown.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(comandasFiltradas) { comanda ->
                                val isSelecionada = comanda == comandaSelecionada
                                Card(
                                    backgroundColor = if (isSelecionada) BackgroundCream else SurfaceWhite,
                                    elevation = if (isSelecionada) 8.dp else 2.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().clickable { comandaSelecionada = comanda }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(comanda.mesa.uppercase(), color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(comanda.dataFechamento ?: "Data não registrada", color = TextDarkBrown, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("R$ ${"%.2f".format(comanda.total)}", color = CorVerde, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(comanda.statusComanda, color = TextDarkBrown, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // COLUNA DA DIREITA: DETALHES (Mantida exatamente como você já tinha deixado)
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    if (comandaSelecionada == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Selecione um pedido na lista para ver os detalhes", color = TextDarkBrown, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        val comanda = comandaSelecionada!!

                        Column(modifier = Modifier.weight(1f)) {
                            Text("DETALHES DO PEDIDO", color = TextDarkBrown, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${comanda.mesa} - Cliente: ${comanda.nomeCliente}", color = PrimaryBrown, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                            Divider(color = TextDarkBrown.copy(alpha = 0.1f), thickness = 2.dp, modifier = Modifier.padding(vertical = 24.dp))

                            Text("ITENS CONSUMIDOS:", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(comanda.itens) { item ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${item.quantidade}x  ${item.nome}", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("R$ ${"%.2f".format(item.preco * item.quantidade)}", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        Column {
                            Divider(color = TextDarkBrown.copy(alpha = 0.1f), thickness = 2.dp, modifier = Modifier.padding(vertical = 20.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("TOTAL PAGO:", color = TextDarkBrown, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("R$ ${"%.2f".format(comanda.total)}", color = CorVerde, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("MÉTODO DE PAGAMENTO:", color = TextDarkBrown, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(comanda.metodoPagamento ?: "Não informado", color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { /* TODO: Chamar lógica de impressão */ },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = SecondaryOrange, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Imprimir", modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("REIMPRIMIR COMPROVANTE", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// FUNÇÕES AUXILIARES
// ==========================================

// 1. O Componente de Dropdown agora recebe as opções e devolve qual foi clicada
@Composable
fun FiltroDropdown(label: String, value: String, options: List<String>, onOptionSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontWeight = FontWeight.Medium) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir", tint = TextDarkBrown, modifier = Modifier.clickable { expanded = true }) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = TextDarkBrown,
                focusedBorderColor = PrimaryBrown,
                cursorColor = PrimaryBrown,
                unfocusedBorderColor = TextDarkBrown.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opcao ->
                DropdownMenuItem(onClick = {
                    onOptionSelected(opcao)
                    expanded = false
                }) {
                    Text(opcao, color = TextDarkBrown, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// 2. A lógica que entende as datas que vieram do banco de dados e as compara com hoje
fun isDataDentroDoFiltro(dataFechamento: String?, filtro: String): Boolean {
    if (filtro == "TUDO") return true
    if (dataFechamento.isNullOrBlank() || dataFechamento.contains("não registrada", ignoreCase = true)) return false

    val dataReal: LocalDate? = try {
        when {
            dataFechamento.contains("às") -> {
                // Converte datas amigáveis como "21/05/2026 às 15:54"
                val textoLimpo = dataFechamento.replace(" às ", " ")
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                LocalDateTime.parse(textoLimpo, formatter).toLocalDate()
            }
            dataFechamento.contains("T") -> {
                // Converte datas de computador como "2026-05-21T15:54:00"
                LocalDateTime.parse(dataFechamento).toLocalDate()
            }
            else -> null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    // Se o sistema não conseguir decifrar a data, ele não esconde a venda do seu pai
    if (dataReal == null) return true

    val hoje = LocalDate.now()

    return when (filtro) {
        "HOJE" -> dataReal.isEqual(hoje)
        "ESTA SEMANA" -> {
            val weekFields = WeekFields.of(Locale("pt", "BR"))
            val semanaComanda = dataReal.get(weekFields.weekOfWeekBasedYear())
            val semanaAtual = hoje.get(weekFields.weekOfWeekBasedYear())
            dataReal.year == hoje.year && semanaComanda == semanaAtual
        }
        "ESTE MÊS" -> dataReal.year == hoje.year && dataReal.month == hoje.month
        else -> true
    }
}