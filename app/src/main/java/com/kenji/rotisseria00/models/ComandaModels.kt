package com.kenji.rotisseria00.models

import kotlinx.serialization.Serializable

@Serializable
data class Comanda(
    val mesa: String,
    val nomeCliente: String,
    val itens: List<ItemComanda>,
    val total: Double,
    val statusComanda: String,
    val metodoPagamento: String? = null,
    val dataFechamento: String? = null,
    val dataEnvioCozinha: String? = null // Horário em que o pedido foi para a cozinha
)

@Serializable
data class ItemComanda(
    val quantidade: Int,
    val nome: String,
    val preco: Double,
    val statusCozinha: String
)

@Serializable
data class PagamentoRequest(
    val metodoPagamento: String,
    val nomeCliente: String?,
    val valorFinal: Double?
)