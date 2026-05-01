package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseria00.models.DashboardResumo
import com.kenji.rotisseria00.models.ItemCardapioResponse
import com.kenji.rotisseria00.network.RotisseriaApi
import kotlinx.coroutines.delay

@Composable
fun NovoDashboardScreen() {
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)
    val corFundoCard = Color(0xFF362511)
    val corAlerta = Color(0xFFD32F2F)

    var resumo by remember { mutableStateOf<DashboardResumo?>(null) }

    // 👇 NOVO ESTADO: Puxa o cardápio para mostrar a tabela de escassez 👇
    var cardapioReal by remember { mutableStateOf<List<ItemCardapioResponse>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }

    // Atualização em tempo real (Loop a cada 5s)
    LaunchedEffect(Unit) {
        while (true) {
            val dadosResumo = RotisseriaApi.buscarResumoDashboard()
            // Atualiza o cardápio também, para a tabela baixar os números sozinha!
            val dadosCardapio = RotisseriaApi.buscarCardapio()

            if (dadosResumo != null) {
                resumo = dadosResumo
                cardapioReal = dadosCardapio
                isLoading = false
            }
            delay(5000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(corFundoApp).padding(32.dp)) {
        Text("Centro de Comando", color = corTextoDestaque, fontSize = 32.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = corTextoDestaque) }
        } else {
            resumo?.let { dados ->
                // Permite rolar a tela se o tablet for menor
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // --- SESSÃO 1: FINANCEIRO ---
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = corTextoDestaque), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            Row(modifier = Modifier.fillMaxSize().padding(32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("FATURAMENTO HOJE", color = corFundoCard, fontSize = 18.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                    Text("R$ ${"%.2f".format(dados.vendasHoje)}", color = corFundoCard, fontSize = 48.sp, fontWeight = FontWeight.Bold, fontFamily = FidalgaFont)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Ticket Médio: R$ ${"%.2f".format(dados.ticketMedio)}", color = corFundoCard.copy(alpha = 0.8f), fontSize = 18.sp, fontFamily = FidalgaFont)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.background(corFundoCard, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text("${dados.pedidosHoje} Pedidos Concluídos", color = corTextoDestaque, fontSize = 16.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("RAIO-X DA OPERAÇÃO", color = corTextoClaro, fontSize = 20.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // --- SESSÃO 2: RESUMOS DOS SETORES ---
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Card Caixa
                            CardResumoSetor(
                                titulo = "CAIXA E SALÃO", icone = Icons.Default.PointOfSale, corFundo = corFundoCard, corTexto = corTextoClaro, modifier = Modifier.weight(1f),
                                infoPrincipal = "${dados.mesasAbertas} Mesas em Aberto",
                                infoSecundaria = "${dados.aguardandoPagamento} Aguardando Pagamento",
                                corDestaque = if (dados.aguardandoPagamento > 0) Color(0xFF388E3C) else corTextoClaro // Fica verde se tem grana pra receber
                            )

                            // Card Fiados
                            CardResumoSetor(
                                titulo = "CADERNETA (FIADOS)", icone = Icons.Default.MoneyOff, corFundo = corFundoCard, corTexto = corTextoClaro, modifier = Modifier.weight(1f),
                                infoPrincipal = "R$ ${"%.2f".format(dados.valorFiados)} a receber",
                                infoSecundaria = "${dados.qtdFiados} Contas Pendentes",
                                corDestaque = if (dados.qtdFiados > 0) corAlerta else corTextoClaro // Fica vermelho se tem dívida
                            )

                            // Card Estoque
                            CardResumoSetor(
                                titulo = "ESTOQUE", icone = Icons.Default.Inventory, corFundo = corFundoCard, corTexto = corTextoClaro, modifier = Modifier.weight(1f),
                                infoPrincipal = if (dados.estoqueBaixo == 0) "Estoque Saudável" else "Atenção Necessária",
                                infoSecundaria = "${dados.estoqueBaixo} Itens na quantidade mínima",
                                corDestaque = if (dados.estoqueBaixo > 0) corAlerta else corTextoDestaque
                            )
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // --- 👇 SESSÃO 3: TABELA DE ESCASSEZ (NOVO) 👇 ---
                    item {
                        // Filtra apenas os pratos que têm um limite de estoque definido e estão disponíveis
                        val pratosComLimite = cardapioReal.filter { it.estoqueAtual != null && it.disponivel }

                        if (pratosComLimite.isNotEmpty()) {
                            Text("CONTROLE DE PRODUÇÃO (PRATOS DO DIA)", color = corTextoClaro, fontSize = 20.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = corFundoCard),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    pratosComLimite.forEachIndexed { index, prato ->
                                        val estoqueAtual = prato.estoqueAtual ?: 0
                                        val limiteOriginal = prato.limiteDiario ?: 0
                                        val esgotado = estoqueAtual <= 0

                                        // Calcula a porcentagem para mudar de cor se estiver acabando (menos de 20%)
                                        val emAlerta = estoqueAtual > 0 && (estoqueAtual.toFloat() / limiteOriginal.toFloat()) <= 0.2f

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = prato.nome, color = corTextoClaro, fontSize = 20.sp, fontFamily = FidalgaFont)

                                            // Barra visual simples
                                            Box(modifier = Modifier.background(
                                                when {
                                                    esgotado -> Color.Red.copy(alpha = 0.2f)
                                                    emAlerta -> corTextoDestaque.copy(alpha = 0.2f)
                                                    else -> Color(0xFF388E3C).copy(alpha = 0.2f)
                                                },
                                                RoundedCornerShape(8.dp)
                                            ).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                                Text(
                                                    text = if (esgotado) "ESGOTADO" else "Restam $estoqueAtual de $limiteOriginal",
                                                    color = when {
                                                        esgotado -> Color(0xFFFF6B6B) // Vermelho claro
                                                        emAlerta -> corTextoDestaque     // Amarelo
                                                        else -> Color(0xFF4CAF50)     // Verde claro
                                                    },
                                                    fontSize = 16.sp,
                                                    fontFamily = FidalgaFont,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        if (index < pratosComLimite.size - 1) {
                                            Divider(color = Color(0xFF5A4A32), thickness = 1.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Espaço extra no final da rolagem
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// Componente para não repetir código visual nos quadradinhos
@Composable
fun CardResumoSetor(titulo: String, icone: ImageVector, corFundo: Color, corTexto: Color, corDestaque: Color, infoPrincipal: String, infoSecundaria: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = corFundo), shape = RoundedCornerShape(24.dp), modifier = modifier.height(180.dp)) {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icone, contentDescription = null, tint = corTexto.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(titulo, color = corTexto.copy(alpha = 0.5f), fontSize = 14.sp, fontFamily = FidalgaFont, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(infoPrincipal, color = corDestaque, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FidalgaFont)
            Spacer(modifier = Modifier.height(4.dp))
            Text(infoSecundaria, color = corTexto, fontSize = 16.sp, fontFamily = FidalgaFont)
        }
    }
}