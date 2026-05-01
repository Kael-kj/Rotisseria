package com.kenji.rotisseria00.models

import kotlinx.serialization.Serializable

@Serializable
data class ItemEstoqueResponse(
    val id: String,
    val nome: String,
    val quantidadeAtual: Double,
    val quantidadeMinima: Double,
    val unidade: String,
    val ultimaCompra: String
)

@Serializable
data class ItemEstoqueRequest(
    val nome: String,
    val quantidadeAtual: Double,
    val quantidadeMinima: Double,
    val unidade: String,
    val ultimaCompra: String
)