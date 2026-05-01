package com.kenji.rotisseria00.models

import kotlinx.serialization.Serializable

@Serializable
data class ItemCardapioResponse(
    val id: String,
    val nome: String,
    val categoria: String,
    val preco: Double,
    val disponivel: Boolean,
    val parceria: String,
    val limiteDiario: Int? = null,
    val estoqueAtual: Int? = null
)

@Serializable
data class ItemCardapioRequest(
    val nome: String,
    val categoria: String,
    val preco: Double,
    val disponivel: Boolean,
    val parceria: String,
    val limiteDiario: Int? = null,
    val estoqueAtual: Int? = null
)