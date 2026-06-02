package com.kenji.rotisseriaadmin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseriaadmin.data.RotisseriaApi
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

// ==========================================
// NOVA ESTRUTURA DE DADOS
// ==========================================
data class DetalheItemFechamento(
    val nome: String,
    var quantidadeVendida: Int,
    var valorTotalArrecadado: Double
)

@Composable
fun FechamentoCaixaScreen(onVoltar: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    // Novo Mapa: Dia -> (Categoria -> (Nome do Item -> Detalhes))
    var fechamentoPorDia by remember { mutableStateOf<Map<String, Map<String, Map<String, DetalheItemFechamento>>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var mensagemExportacao by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val historico = RotisseriaApi.buscarHistorico()
            val cardapio = RotisseriaApi.buscarCardapio()
            val mapaCategorias = cardapio.associate { it.nome to it.categoria }

            // Lógica de agrupamento profundo (Dia > Categoria > Itens)
            val dadosAgrupados = mutableMapOf<String, MutableMap<String, MutableMap<String, DetalheItemFechamento>>>()

            for (comanda in historico) {
                // Proteção contra data nula que fizemos antes
                val dataLimpa = (comanda.dataFechamento ?: "Sem Data").substringBefore(" ").trim()
                if (dataLimpa.isEmpty() || dataLimpa == "Data") continue

                val categoriasDoDia = dadosAgrupados.getOrPut(dataLimpa) { mutableMapOf() }

                for (item in comanda.itens) {
                    val categoria = mapaCategorias[item.nome] ?: "Outros / Não Categorizado"
                    val itensDaCategoria = categoriasDoDia.getOrPut(categoria) { mutableMapOf() }

                    // Se o item já existe nessa categoria, soma a quantidade e valor. Se não, cria um novo.
                    val detalheAtual = itensDaCategoria.getOrPut(item.nome) {
                        DetalheItemFechamento(item.nome, 0, 0.0)
                    }

                    detalheAtual.quantidadeVendida += item.quantidade
                    detalheAtual.valorTotalArrecadado += (item.preco * item.quantidade)
                }
            }

            fechamentoPorDia = dadosAgrupados.toSortedMap(compareByDescending { it })
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(CorFundoApp).padding(32.dp)) {
        // CABEÇALHO E BOTÃO DE EXPORTAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVoltar) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = CorTextoEscuro)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fechamento de Caixa (Sócios)", color = CorTextoEscuro, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val caminhoSalvo = gerarPlanilhaExcelDetalhada(fechamentoPorDia)
                            mensagemExportacao = "✅ SUCESSO!\n\nPlanilha salva em:\n$caminhoSalvo"
                        } catch (e: Exception) {
                            mensagemExportacao = "❌ Erro ao salvar!\n\nFeche o Excel se estiver aberto e tente de novo.\nDetalhe: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = CorVerde, contentColor = Color.White)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXPORTAR PARA EXCEL", fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CorTextoEscuro)
            }
        } else if (fechamentoPorDia.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum dado de fechamento encontrado.", color = CorTextoEscuro, fontSize = 20.sp)
            }
        } else {
            // LISTA DE DIAS
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(fechamentoPorDia.entries.toList()) { (data, categorias) ->

                    // Soma de todas as categorias para descobrir o total do dia
                    val totalDoDia = categorias.values.sumOf { itensMap -> itensMap.values.sumOf { it.valorTotalArrecadado } }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = CorFundoCard,
                        elevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                            // TOTAL DO DIA
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Data: $data", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CorTextoEscuro)
                                Text("Total: R$ ${"%.2f".format(totalDoDia)}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CorVerde)
                            }

                            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

                            // LISTA EXPANSÍVEL DE CATEGORIAS
                            categorias.forEach { (nomeCategoria, itensDaCategoria) ->
                                CategoriaExpansivel(nomeCategoria, itensDaCategoria)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Aviso Exportação
    mensagemExportacao?.let { mensagem ->
        AlertDialog(
            onDismissRequest = { mensagemExportacao = null },
            title = { Text("Aviso", fontWeight = FontWeight.Bold, color = CorTextoEscuro) },
            text = { Text(mensagem) },
            confirmButton = {
                Button(onClick = { mensagemExportacao = null }, colors = ButtonDefaults.buttonColors(backgroundColor = CorTextoEscuro)) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }
}

// ==========================================
// COMPONENTE: CATEGORIA QUE ABRE E FECHA (SANFONA)
// ==========================================
@Composable
fun CategoriaExpansivel(nomeCategoria: String, itensMap: Map<String, DetalheItemFechamento>) {
    var expandido by remember { mutableStateOf(false) }
    val totalDaCategoria = itensMap.values.sumOf { it.valorTotalArrecadado }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // LINHA DA CATEGORIA (CLICÁVEL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expandido = !expandido }
                .background(if (expandido) CorDestaque.copy(alpha = 0.2f) else Color.Transparent)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CorTextoEscuro
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(nomeCategoria.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CorTextoEscuro)
            }
            Text("R$ ${"%.2f".format(totalDaCategoria)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CorTextoEscuro)
        }

        // LISTA DE ITENS DENTRO DA CATEGORIA (Só aparece se clicar)
        AnimatedVisibility(visible = expandido) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 40.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
                // Título das sub-colunas
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Item", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(2f))
                    Text("Qtd", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                    Text("Valor (R$)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }

                // Os itens vendidos
                itensMap.values.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.nome, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CorTextoEscuro, modifier = Modifier.weight(2f))
                        Text("${item.quantidadeVendida}x", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CorTextoEscuro, modifier = Modifier.weight(1f))
                        Text("R$ ${"%.2f".format(item.valorTotalArrecadado)}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = CorTextoEscuro, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
                }
            }
        }
    }
}

// ==========================================
// EXCEL TURBINADO: AGORA MOSTRA OS ITENS
// ==========================================
fun gerarPlanilhaExcelDetalhada(dados: Map<String, Map<String, Map<String, DetalheItemFechamento>>>): String {
    val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
    val sheet = workbook.createSheet("Fechamento Rotisseria")

    // Estilos
    val headerStyle = workbook.createCellStyle()
    val font = workbook.createFont().apply { bold = true }
    headerStyle.setFont(font)

    val subTotalStyle = workbook.createCellStyle()
    val fontSub = workbook.createFont().apply { bold = true; color = org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.index }
    subTotalStyle.setFont(fontSub)

    // Cabeçalhos (Aumentamos as colunas)
    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).apply { setCellValue("Data"); cellStyle = headerStyle }
    headerRow.createCell(1).apply { setCellValue("Categoria"); cellStyle = headerStyle }
    headerRow.createCell(2).apply { setCellValue("Item Vendido"); cellStyle = headerStyle }
    headerRow.createCell(3).apply { setCellValue("Qtd."); cellStyle = headerStyle }
    headerRow.createCell(4).apply { setCellValue("Faturamento (R$)"); cellStyle = headerStyle }

    var numeroLinha = 1

    for ((data, categorias) in dados) {
        var totalDoDia = 0.0

        for ((categoria, itensDaCategoria) in categorias) {
            var totalDaCategoria = 0.0

            // Lista os itens específicos
            for (item in itensDaCategoria.values) {
                val row = sheet.createRow(numeroLinha++)
                row.createCell(0).setCellValue(data)
                row.createCell(1).setCellValue(categoria)
                row.createCell(2).setCellValue(item.nome)
                row.createCell(3).setCellValue(item.quantidadeVendida.toDouble())
                row.createCell(4).setCellValue(item.valorTotalArrecadado)

                totalDaCategoria += item.valorTotalArrecadado
                totalDoDia += item.valorTotalArrecadado
            }

            // Subtotal da Categoria
            val catRow = sheet.createRow(numeroLinha++)
            catRow.createCell(1).apply { setCellValue(">> SUBTOTAL $categoria:"); cellStyle = subTotalStyle }
            catRow.createCell(4).apply { setCellValue(totalDaCategoria); cellStyle = subTotalStyle }
        }

        // Subtotal do Dia inteiro
        val diaRow = sheet.createRow(numeroLinha++)
        diaRow.createCell(1).apply { setCellValue("TOTAL DO DIA $data:"); cellStyle = headerStyle }
        diaRow.createCell(4).apply { setCellValue(totalDoDia); cellStyle = headerStyle }

        numeroLinha++ // Espaço em branco entre os dias
    }

    // Autoajuste das 5 colunas
    for (i in 0..4) sheet.autoSizeColumn(i)

    // Mágica para salvar na Área de Trabalho
    val caminhoDesktop = javax.swing.filechooser.FileSystemView.getFileSystemView().homeDirectory
    val arquivoExcel = java.io.File(caminhoDesktop, "Fechamento_Rotisseria_Detalhado.xlsx")

    java.io.FileOutputStream(arquivoExcel).use { fileOut ->
        workbook.write(fileOut)
    }
    workbook.close()

    return arquivoExcel.absolutePath
}