package com.kenji.rotisseriaadmin.data

// ==========================================
// MODELOS COMPLEMENTARES ANINHADOS
// ==========================================

@kotlinx.serialization.Serializable
data class IngredienteNoPrato(
    val nomeIngrediente: String,
    val quantidadeNecessaria: Double
)

@kotlinx.serialization.Serializable
data class ItemComanda(
    val quantidade: Int,
    val nome: String,
    val preco: Double,
    val statusCozinha: String,
    val categoria: String = "Geral",
    val parceria: String = "Nenhuma"
)

@kotlinx.serialization.Serializable // ou apenas data class se usar Gson
data class Comanda(
    val mesa: String,
    val nomeCliente: String,
    val itens: List<ItemComanda>,
    val total: Double,
    val statusComanda: String,
    val metodoPagamento: String? = null,
    val dataFechamento: String? = null,
    val dataEnvioCozinha: String? = null
)
// ==========================================
// RESPOSTAS DO SERVIDOR (RESPONSES)
// ==========================================

@kotlinx.serialization.Serializable
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

@kotlinx.serialization.Serializable
data class ComandaResponse(
    val mesa: String,
    val nomeCliente: String,
    val itens: List<ItemComanda>,
    val total: Double,
    val statusComanda: String,
    val metodoPagamento: String? = null,
    val dataFechamento: String? = null,
    val dataEnvioCozinha: String? = null
)

@kotlinx.serialization.Serializable
data class ItemCardapioResponse(
    val id: String,
    val nome: String,
    val categoria: String,
    val preco: Double,
    val disponivel: Boolean,
    val parceria: String,
    val limiteDiario: Int? = null,
    val estoqueAtual: Int? = null,
    val ingredientes: List<IngredienteNoPrato> = emptyList()
)

@kotlinx.serialization.Serializable
data class ItemEstoqueResponse(
    val id: String,
    val nome: String,
    val quantidadeAtual: Double,
    val quantidadeMinima: Double,
    val unidade: String,
    val ultimaCompra: String
)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val sucesso: Boolean,
    val perfil: String? = null,
    val mensagem: String
)

// ==========================================
// REQUISIÇÕES PARA O SERVIDOR (REQUESTS)
// ==========================================

@kotlinx.serialization.Serializable
data class ItemEstoqueRequest(
    val nome: String,
    val quantidadeAtual: Double,
    val quantidadeMinima: Double,
    val unidade: String,
    val ultimaCompra: String
)

@kotlinx.serialization.Serializable
data class ItemCardapioRequest(
    val nome: String,
    val categoria: String,
    val preco: Double,
    val disponivel: Boolean,
    val parceria: String,
    val limiteDiario: Int? = null,
    val estoqueAtual: Int? = null,
    val ingredientes: List<IngredienteNoPrato> = emptyList()
)

@kotlinx.serialization.Serializable
data class PagamentoRequest(
    val metodoPagamento: String,
    val nomeCliente: String? = null,
    val valorFinal: Double? = null
)

@kotlinx.serialization.Serializable
data class LoginRequest(
    val usuario: String,
    val senha: String
)
