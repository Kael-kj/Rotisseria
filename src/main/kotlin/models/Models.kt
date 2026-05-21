package com.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.bson.codecs.kotlinx.ObjectIdSerializer
import org.bson.types.ObjectId

@Serializable
data class ItemCardapio(
    @SerialName("_id")
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId? = null,
    val nome: String,
    val categoria: String,
    val preco: Double,
    val disponivel: Boolean,
    val parceria: String,
    val limiteDiario: Int? = null,
    val estoqueAtual: Int? = null,
    val ingredientes: List<IngredienteNoPrato> = emptyList()
)

@Serializable
data class IngredienteNoPrato(
    val nomeIngrediente: String,
    val quantidadeNecessaria: Double
)

@Serializable
data class ItemEstoque(
    @SerialName("_id")
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId? = null,
    val nome: String,
    val quantidadeAtual: Double,
    val quantidadeMinima: Double,
    val unidade: String,
    val ultimaCompra: String
)

@Serializable
data class Comanda(
    val mesa: String,
    val nomeCliente: String,
    val itens: List<ItemComanda>,
    val total: Double,
    val statusComanda: String,
    val metodoPagamento: String? = null,
    val dataFechamento: String? = null,
    val dataEnvioCozinha: String? = null // <- NOVO
)

@Serializable
data class ItemComanda(
    val quantidade: Int,
    val nome: String,
    val preco: Double,
    val statusCozinha: String,
    val categoria: String = "Geral",
    val parceria: String = "Nenhuma"
)

// Classes de Resposta Limpas (para o App e Painel)
@Serializable
data class ItemCardapioResponse(
    val id: String, val nome: String, val categoria: String, val preco: Double,
    val disponivel: Boolean, val parceria: String, val limiteDiario: Int?,
    val estoqueAtual: Int?, val ingredientes: List<IngredienteNoPrato>
)

@Serializable
data class ItemEstoqueResponse(
    val id: String,
    val nome: String,
    val quantidadeAtual: Double,
    val quantidadeMinima: Double, // Faltava este parâmetro
    val unidade: String,
    val ultimaCompra: String      // Faltava este parâmetro também
)

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

@Serializable
data class PagamentoRequest(
    val metodoPagamento: String,
    val nomeCliente: String? = null,
    val valorFinal: Double? = null
)

@Serializable
data class LoginResponse(
    val sucesso: Boolean,
    val perfil: String? = null,
    val mensagem: String
)

@Serializable
data class ItemEstoqueRequest(
    val nome: String,
    val quantidadeAtual: Double,
    val quantidadeMinima: Double,
    val unidade: String,
    val ultimaCompra: String
)

@Serializable
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

@Serializable
data class Usuario(
    @SerialName("_id")
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId? = null,
    val usuario: String, // Antes a gente tinha chamado de "login"
    val senha: String,
    val perfil: String   // Adicionamos o perfil que faltava!
)

@Serializable
data class LoginRequest(
    val usuario: String, // Antes estava "login"
    val senha: String
)

@Serializable
data class ComandaResponse(
    val mesa: String,
    val nomeCliente: String,
    val itens: List<ItemComanda>,
    val total: Double,
    val statusComanda: String,
    val metodoPagamento: String? = null,
    val dataFechamento: String? = null,
    val dataEnvioCozinha: String? = null // <- NOVO
)
