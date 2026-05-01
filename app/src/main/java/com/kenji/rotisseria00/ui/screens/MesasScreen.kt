package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseria00.R
import io.ktor.client.request.invoke


val FidalgaFont = FontFamily(
    Font(R.font.imfeensc28p)
)

@Composable
fun MesasScreen(onMesaClick: (String) -> Unit) {
    val corFundoHeader = Color(0xFF5A4A32)
    val corFundoApp = Color(0xFF432F17)
    val corTextoDestaque = Color(0xFFF8CE6A)
    val corTextoClaro = Color(0xFFEBE1CE)


    val quantidadeTotalDeMesas = 12

    val mesasOcupadas = emptyList<Int>()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(corFundoApp)
    ) {
        // --- HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = corFundoHeader,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ROTISSERIA DA ROÇA",
                color = corTextoDestaque,
                fontSize = 28.sp,
                fontFamily = FidalgaFont // Usando a sua fonte!
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- TÍTULO ---
        Text(
            text = "ESCOLHA A MESA",
            color = corTextoClaro,
            fontSize = 22.sp,
            fontFamily = FidalgaFont, // Usando a sua fonte!
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- GRADE DINÂMICA DE MESAS ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(quantidadeTotalDeMesas) { index ->
                val numeroMesa = index + 1
                val numeroMesaFormatado = numeroMesa.toString().padStart(2, '0')

                // LÓGICA NOVA: A mesa está ocupada se existir uma lista de itens para ela no ControlePedidos
                val estaOcupada = ControlePedidos.comandasAbertas[numeroMesaFormatado]?.isNotEmpty() == true

                MesaCard(
                    numero = numeroMesaFormatado,
                    ocupada = estaOcupada,
                    onClick = {
                        onMesaClick(numeroMesaFormatado)
                    }
                )
            }
        }
    }
}

// --- COMPONENTE DO CARTÃO ---
@Composable
fun MesaCard(numero: String, ocupada: Boolean, onClick: () -> Unit) {
    val corFundoCard = if (ocupada) Color(0xFFF8CE6A) else Color(0xFFEBE1CE)
    val corTextoCard = Color(0xFF432F17)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .background(corFundoCard, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MESA",
                color = corTextoCard,
                fontSize = 12.sp,
                fontFamily = FidalgaFont // Usando a sua fonte!
            )

            Text(
                text = numero,
                color = corTextoCard,
                fontSize = 36.sp,
                fontFamily = FidalgaFont // Usando a sua fonte!
            )

            if (ocupada) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ocupada",
                        color = corTextoCard,
                        fontSize = 12.sp,
                        fontFamily = FidalgaFont // Usando a sua fonte!
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.Schedule, // O relógio original voltou!
                        contentDescription = "Ocupada",
                        tint = corTextoCard,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}