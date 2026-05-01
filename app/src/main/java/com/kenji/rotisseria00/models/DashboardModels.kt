package com.kenji.rotisseria00.models

import kotlinx.serialization.Serializable

@Serializable
data class DashboardResumo(
    val vendasHoje: Double,
    val pedidosHoje: Int,
    val ticketMedio: Double,
    val estoqueBaixo: Int,
    val mesasAbertas: Int,
    val aguardandoPagamento: Int,
    val qtdFiados: Int,
    val valorFiados: Double
)