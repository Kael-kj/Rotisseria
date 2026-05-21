package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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

// Modelo simples para o agrupamento na tela
data class ClienteDevedor(
    val nome: String,
    val totalDevedor: Double,
    val quantidadePedidos: Int,
    val comandas: List<ComandaResponse>
)

@Composable
fun FiadosScreen() {
    val coroutineScope = rememberCoroutineScope()

    // Estados dos Dados
    var devedores by remember { mutableStateOf<List<ClienteDevedor>>(emptyList()) }
    var clienteSelecionado by remember { mutableStateOf<ClienteDevedor?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados de Interface
    var searchQuery by remember { mutableStateOf("") }
    var processandoQuitacao by remember { mutableStateOf(false) }
    var mensagemSucesso by remember { mutableStateOf(false) }
    var metodoPagamento by remember { mutableStateOf("Pix") }
    var erroConexao by remember { mutableStateOf<String?>(null) }

    // Função para buscar dados e agrupar por nome do cliente
    fun carregarDados() {
        coroutineScope.launch {
            isLoading = true
            try {
                val todasComandas = RotisseriaApi.buscarFiados()
                // AGRUPAMENTO EM TEMPO REAL NO FRONTEND
                devedores = todasComandas.groupBy { it.nomeCliente.trim().uppercase() }
                    .map { (nome, lista) ->
                        ClienteDevedor(nome, lista.sumOf { it.total }, lista.size, lista)
                    }.sortedBy { it.nome }
            } catch (e: Exception) {
                erroConexao = "Erro ao carregar fiados."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { carregarDados() }

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (isLoading) {
            CircularProgressIndicator(color = PrimaryBrown, modifier = Modifier.align(Alignment.Center))
        } else if (mensagemSucesso) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✅ TODAS AS DÍVIDAS DE ${clienteSelecionado?.nome} FORAM QUITADAS!", color = CorVerde, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { mensagemSucesso = false; clienteSelecionado = null; carregarDados() }) {
                    Text("VOLTAR PARA A LISTA", modifier = Modifier.padding(8.dp))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {

                // COLUNA ESQUERDA: LISTA DE CLIENTES
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("Caderneta de Fiados", color = TextDarkBrown, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar Cliente") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = PrimaryBrown)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val filtrados = devedores.filter { it.nome.contains(searchQuery, ignoreCase = true) }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filtrados) { devedor ->
                            val isAtivo = devedor.nome == clienteSelecionado?.nome
                            Card(
                                backgroundColor = if (isAtivo) BackgroundCream else SurfaceWhite,
                                shape = RoundedCornerShape(12.dp),
                                elevation = if (isAtivo) 4.dp else 1.dp,
                                modifier = Modifier.fillMaxWidth().clickable { clienteSelecionado = devedor }
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = if(isAtivo) PrimaryBrown else TextDarkBrown.copy(0.4f))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(devedor.nome, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDarkBrown)
                                        Text("${devedor.quantidadePedidos} pedidos pendentes", fontSize = 13.sp, color = TextDarkBrown.copy(0.6f))
                                    }
                                    Text("R$ ${"%.2f".format(devedor.totalDevedor)}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CorAlerta)
                                }
                            }
                        }
                    }
                }

                // COLUNA DIREITA: DETALHES E QUITAÇÃO EM MASSA
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    if (clienteSelecionado == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Selecione um cliente para ver o extrato", color = TextDarkBrown.copy(0.3f))
                        }
                    } else {
                        val cliente = clienteSelecionado!!

                        Column(modifier = Modifier.weight(1f)) {
                            Text("EXTRATO DE DÍVIDAS", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(cliente.nome, color = PrimaryBrown, fontWeight = FontWeight.Bold, fontSize = 24.sp)

                            Divider(modifier = Modifier.padding(vertical = 16.dp), color = SurfaceWhite, thickness = 2.dp)

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(cliente.comandas) { comanda ->
                                    Column(modifier = Modifier.fillMaxWidth().background(SurfaceWhite.copy(0.5f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Data: ${comanda.dataFechamento ?: "N/A"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("R$ ${"%.2f".format(comanda.total)}", color = CorAlerta, fontWeight = FontWeight.Bold)
                                        }
                                        comanda.itens.forEach { item ->
                                            Text("- ${item.quantidade}x ${item.nome}", fontSize = 12.sp, color = TextDarkBrown.copy(0.7f))
                                        }
                                    }
                                }
                            }
                        }

                        Column {
                            Divider(modifier = Modifier.padding(vertical = 16.dp), color = SurfaceWhite, thickness = 2.dp)
                            Text("MÉTODO DE QUITAÇÃO:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Dinheiro", "Pix", "Cartão").forEach { m ->
                                    val ativo = metodoPagamento == m
                                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                        .background(if(ativo) CorVerde else SurfaceWhite)
                                        .clickable { metodoPagamento = m }.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) { Text(m, color = if(ativo) OnPrimary else TextDarkBrown, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL ACUMULADO:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("R$ ${"%.2f".format(cliente.totalDevedor)}", color = CorAlerta, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        processandoQuitacao = true
                                        // CHAMADA REAL: Quita todos os pedidos do cliente de uma vez
                                        val sucesso = RotisseriaApi.quitarTodosFiados(cliente.nome, metodoPagamento.uppercase())
                                        if (sucesso) mensagemSucesso = true
                                        else erroConexao = "Falha ao comunicar com servidor."
                                        processandoQuitacao = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = CorVerde, contentColor = OnPrimary),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !processandoQuitacao
                            ) {
                                if (processandoQuitacao) CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(24.dp))
                                else Text("QUITAR TODO O SALDO", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}