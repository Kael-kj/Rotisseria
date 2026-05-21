package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseriaadmin.data.DashboardResumo
import com.kenji.rotisseriaadmin.data.ItemCardapioResponse
import com.kenji.rotisseriaadmin.data.RotisseriaApi
import com.kenji.rotisseriaadmin.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen() {
    val corAlerta = Color(0xFFD32F2F)
    val corSucesso = Color(0xFF388E3C)

    var resumo by remember { mutableStateOf<DashboardResumo?>(null) }
    var cardapioReal by remember { mutableStateOf<List<ItemCardapioResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Ciclo de atualização em tempo real (Loop a cada 5 segundos)
    LaunchedEffect(Unit) {
        while (true) {
            val dadosResumo = RotisseriaApi.buscarResumoDashboard()
            val dadosCardapio = RotisseriaApi.buscarCardapio()
            if (dadosResumo != null) {
                resumo = dadosResumo
                cardapioReal = dadosCardapio
                isLoading = false
            }
            delay(5000)
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBrown)
            }
        } else {
            resumo?.let { dados ->
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {

                    // TÍTULO DA TELA
                    item {
                        Text(
                            text = "Centro de Comando",
                            color = TextDarkBrown,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // BANNER PRINCIPAL: FATURAMENTO (Destaque em Marrom e Laranja)
                    item {
                        Card(
                            backgroundColor = PrimaryBrown,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text("FATURAMENTO HOJE", color = OnPrimary.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text("R$ ${"%.2f".format(dados.vendasHoje)}", color = OnPrimary, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                                    Text("Ticket Médio: R$ ${"%.2f".format(dados.ticketMedio)}", color = OnPrimary.copy(alpha = 0.8f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(modifier = Modifier.background(SecondaryOrange, RoundedCornerShape(8.dp)).padding(horizontal = 20.dp, vertical = 10.dp)) {
                                        Text("${dados.pedidosHoje} PEDIDOS CONCLUÍDOS", color = OnSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // RAIO-X DA OPERAÇÃO (Cards em Branco com Texto Escuro)
                    item {
                        Text("RAIO-X DA OPERAÇÃO", color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CardResumoSetor(
                                titulo = "CAIXA E SALÃO",
                                icone = Icons.Default.PointOfSale,
                                infoPrincipal = "${dados.mesasAbertas} MESAS EM ABERTO",
                                infoSecundaria = "${dados.aguardandoPagamento} Aguardando Pagamento",
                                corDestaque = corSucesso,
                                modifier = Modifier.weight(1f)
                            )
                            CardResumoSetor(
                                titulo = "CADERNETA (FIADOS)",
                                icone = Icons.Default.MoneyOff,
                                infoPrincipal = "R$ ${"%.2f".format(dados.valorFiados)} A RECEBER",
                                infoSecundaria = "${dados.qtdFiados} Contas Pendentes",
                                corDestaque = if (dados.qtdFiados > 0) corAlerta else TextDarkBrown,
                                modifier = Modifier.weight(1f)
                            )
                            CardResumoSetor(
                                titulo = "ESTOQUE",
                                icone = Icons.Default.Inventory,
                                infoPrincipal = if (dados.estoqueBaixo == 0) "Estoque Saudável" else "Atenção Necessária",
                                infoSecundaria = "${dados.estoqueBaixo} Itens na quantidade mínima",
                                corDestaque = if (dados.estoqueBaixo > 0) corAlerta else corSucesso,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // CONTROLE DE PRODUÇÃO (Tabela Limpa)
                    item {
                        val pratosComLimite = cardapioReal.filter { it.estoqueAtual != null && it.disponivel }
                        if (pratosComLimite.isNotEmpty()) {
                            Text("CONTROLE DE PRODUÇÃO (PRATOS DO DIA)", color = TextDarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))

                            Card(backgroundColor = SurfaceWhite, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), elevation = 1.dp) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    pratosComLimite.forEachIndexed { index, prato ->
                                        val estoqueAtual = prato.estoqueAtual ?: 0
                                        val limiteOriginal = prato.limiteDiario ?: 0

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = prato.nome, color = TextDarkBrown, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                            Box(modifier = Modifier.background(BackgroundCream, RoundedCornerShape(6.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                                                Text(text = "RESTAM $estoqueAtual DE $limiteOriginal", color = PrimaryBrown, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (index < pratosComLimite.size - 1) {
                                            Divider(color = BackgroundCream, thickness = 2.dp)
                                        }
                                    }
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
fun CardResumoSetor(titulo: String, icone: ImageVector, infoPrincipal: String, infoSecundaria: String, corDestaque: Color, modifier: Modifier = Modifier) {
    Card(backgroundColor = SurfaceWhite, shape = RoundedCornerShape(16.dp), modifier = modifier.height(140.dp), elevation = 1.dp) {
        Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icone, contentDescription = null, tint = TextDarkBrown.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = titulo, color = TextDarkBrown.copy(alpha = 0.4f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = infoPrincipal, color = corDestaque, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = infoSecundaria, color = TextDarkBrown.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}