package com.kenji.rotisseriaadmin.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.net.InetAddress
import javax.jmdns.JmDNS
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object RotisseriaApi {
    // 1. Definição da URL (Garanta que o nome seja BASE_URL com maiúsculas)
    private const val BASE_URL = "http://localhost:8080"

    // 2. Definição do CLIENT (O que estava faltando no seu print)
    private val client = HttpClient(io.ktor.client.engine.cio.CIO) {
        install(ContentNegotiation) {
            json(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }

    suspend fun buscarResumoDashboard(): DashboardResumo? {
        return try {
            val response = client.get("$BASE_URL/dashboard/resumo")
            if (response.status == HttpStatusCode.OK) {
                response.body<DashboardResumo>()
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    // ==========================================
    // CAIXA E COMANDAS
    // ==========================================

    suspend fun buscarContasAbertas(): List<ComandaResponse> {
        return try {
            client.get("$BASE_URL/caixa/abertas").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun buscarContasPendentes(): List<ComandaResponse> {
        return try {
            client.get("$BASE_URL/caixa/pendentes").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fecharConta(mesa: String): Boolean {
        return try {
            // Codifica os espaços da mesa (ex: "MESA 1" vira "MESA%201") para a URL não quebrar
            val mesaCodificada = java.net.URLEncoder.encode(mesa, "UTF-8").replace("+", "%20")

            val response = client.put("$BASE_URL/comandas/$mesaCodificada/fechar")

            if (response.status == io.ktor.http.HttpStatusCode.OK) {
                true
            } else {
//                val erroServer = io.ktor.client.statement.bodyAsText(response)
                println("❌ ERRO DO SERVIDOR AO FECHAR CONTA: Status ${response.status} -> ")
                false
            }
        } catch (e: Exception) {
            println("❌ ERRO DE CÓDIGO NO APP: A requisição de fechar conta nem saiu.")
            e.printStackTrace()
            false
        }
    }

    suspend fun confirmarPagamento(mesa: String, metodo: String, valorFinal: Double, nomeFiado: String? = null): Boolean {
        // CORREÇÃO: Codifica o nome da mesa para aceitar acentos e espaços
        val mesaCodificada = mesa.encodeURLPath()

        println("--- LOG DESKTOP: ENVIANDO PAGAMENTO ---")
        println("Mesa Original: $mesa")
        println("Mesa Codificada: $mesaCodificada")

        return try {
            val request = PagamentoRequest(metodo, nomeFiado, valorFinal)
            val response = client.put("$BASE_URL/caixa/pagar/$mesaCodificada") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            println("RESPOSTA DO SERVIDOR: ${response.status}")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            println("ERRO: ${e.message}")
            false
        }
    }

    // ==========================================
    // FIADOS E HISTÓRICO
    // ==========================================

    suspend fun buscarFiados(): List<ComandaResponse> {
        return try {
            client.get("$BASE_URL/fiados").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun quitarFiado(mesa: String, metodo: String): Boolean {
        return try {
            val request = PagamentoRequest(metodo, null, null)
            val response = client.put("$BASE_URL/fiados/quitar/$mesa") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun buscarHistorico(): List<ComandaResponse> {
        return try {
            // Usa o Ktor Client do front para bater na rota que criamos no backend
            client.get("$BASE_URL/historico").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ==========================================
    // ESTOQUE
    // ==========================================

    suspend fun buscarEstoque(): List<ItemEstoqueResponse> {
        return try {
            client.get("$BASE_URL/estoque").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun adicionarEstoque(item: ItemEstoqueRequest): Boolean {
        return try {
            val response = client.post("$BASE_URL/estoque") {
                contentType(ContentType.Application.Json)
                setBody(item)
            }
            response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun atualizarEstoque(id: String, item: ItemEstoqueRequest): Boolean {
        return try {
            val response = client.put("$BASE_URL/estoque/$id") {
                contentType(ContentType.Application.Json)
                setBody(item)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==========================================
    // CARDÁPIO
    // ==========================================

    suspend fun buscarCardapio(): List<ItemCardapioResponse> {
        return try {
            val response = client.get("$BASE_URL/cardapio")
            if (response.status == HttpStatusCode.OK) {
                response.body<List<ItemCardapioResponse>>()
            } else {
                println("Aviso: Servidor retornou erro ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            println("Erro de Conexão no Cardápio: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun adicionarPrato(item: ItemCardapioRequest): Boolean {
        println("--- LOG API: CADASTRANDO NOVO PRATO ---")
        return try {
            val response = client.post("$BASE_URL/cardapio") {
                contentType(ContentType.Application.Json)
                setBody(item)
            }
            println("RESPOSTA SERVIDOR: ${response.status}")
            response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            println("ERRO AO CADASTRAR: ${e.message}")
            false
        }
    }

    suspend fun atualizarPrato(id: String, item: ItemCardapioRequest): Boolean {
        println("--- LOG API: ATUALIZANDO PRATO $id ---")
        return try {
            val response = client.put("$BASE_URL/cardapio/$id") {
                contentType(ContentType.Application.Json)
                setBody(item)
            }
            println("RESPOSTA SERVIDOR: ${response.status}")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            println("ERRO AO ATUALIZAR: ${e.message}")
            false
        }
    }

    // ==========================================
    // CRIAÇÃO DE PEDIDOS (ENVIAR PARA COZINHA)
    // ==========================================

    suspend fun enviarComanda(comanda: Comanda): Pair<Boolean, String?> {
        println("DEBUG: Tentando enviar pedido para $BASE_URL/comandas")
        return try {
            val response = client.post("$BASE_URL/comandas") {
                contentType(ContentType.Application.Json)
                setBody(comanda)
            }

            when (response.status) {
                HttpStatusCode.Created, HttpStatusCode.OK -> {
                    Pair(true, null) // Sucesso total
                }
                HttpStatusCode.Conflict -> {
                    // O servidor barrou por falta de estoque. Lê o JSON de erro.
                    val erroMap = response.body<Map<String, String>>()
                    Pair(false, erroMap["erro"] ?: "Estoque esgotado para algum item.")
                }
                else -> {
                    Pair(false, "Erro no servidor. Tente novamente.")
                }
            }
        } catch (e: Exception) {
            println("DEBUG: ERRO CRÍTICO NA CONEXÃO: ${e.message}")
            Pair(false, "Falha de conexão com o servidor.")
        }
    }

    fun encontrarServidorAutomaticamente(): String? {
        val jmdns = JmDNS.create(InetAddress.getLocalHost())
        val info = jmdns.getServiceInfo("_http._tcp.local.", "rotisseria-server")
        return info?.let { "http://${it.inet4Addresses[0].hostAddress}:8080" }
    }

    suspend fun buscarFiadosAgrupados(): List<Map<String, Any>> {
        return try {
            client.get("$BASE_URL/fiados/agrupados").body()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun quitarTodosFiados(nomeCliente: String, metodo: String): Boolean {
        val nomeCodificado = nomeCliente.trim().encodeURLPath()

        return try {
            val request = PagamentoRequest(metodo, null, null)
            val response = client.put("$BASE_URL/fiados/quitar-todos/$nomeCodificado") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Função para apagar um item da comanda pelo Caixa
    suspend fun cancelarItem(mesa: String, nomeItem: String): Boolean {
        return try {
            // 1. Transforma os espaços em código de URL válido (ex: "Coca%20Cola")
            val mesaCodificada = java.net.URLEncoder.encode(mesa, "UTF-8").replace("+", "%20")
            val nomeCodificado = java.net.URLEncoder.encode(nomeItem, "UTF-8").replace("+", "%20")

            val response = client.delete("$BASE_URL/comandas/$mesaCodificada/item/$nomeCodificado")

            if (response.status == io.ktor.http.HttpStatusCode.OK) {
                true
            } else {
                // Se o servidor Ktor barrar, vai printar o motivo no painel inferior do Android Studio
                println("❌ ERRO DO SERVIDOR AO DELETAR: Status ${response.status} -> ")
                false
            }
        } catch (e: Exception) {
            println("❌ ERRO DE CÓDIGO NO APP: A requisição nem saiu.")
            e.printStackTrace()
            false
        }
    }
}