package com.kenji.rotisseria00.network

import com.kenji.rotisseria00.models.Comanda
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*


object RotisseriaApi {

//    private var BASE_URL = "http://192.168.0.11:8080" // IP padrão para Emulador (No celular real, use o IP do seu PC)
    private var BASE_URL = "http://192.168.0.101:8080" // IP do Cliente

    fun updateBaseUrl(newUrl: String) {
        // Ignora localhost se vier do NSD, pois no Android não serve para o PC
        if (newUrl.contains("127.0.0.1") || newUrl.contains("localhost")) {
            println("Aviso: Ignorando URL de loopback detectada pelo NSD: $newUrl")
            return
        }
        BASE_URL = newUrl
        println("API BASE_URL atualizada para: $BASE_URL")
    }

    // 0. Login Centralizado (Usa a BASE_URL atual)
    suspend fun fazerLogin(usuario: String, senha: String): com.kenji.rotisseria00.models.LoginResponse {
        println("LOGIN: Iniciando tentativa em $BASE_URL/login")
        return try {
            val response = KtorClient.httpClient.post("$BASE_URL/login") {
                contentType(ContentType.Application.Json)
                setBody(com.kenji.rotisseria00.models.LoginRequest(usuario, senha))
            }
            println("LOGIN: Servidor respondeu com status ${response.status}")
            response.body()
        } catch (e: Exception) {
            println("LOGIN: ERRO DE CONEXÃO em $BASE_URL")
            e.printStackTrace()
            com.kenji.rotisseria00.models.LoginResponse(false, null, "Erro ao conectar no servidor local ($BASE_URL).")
        }
    }

    // 1. Envia a comanda para a cozinha e salva no banco
    suspend fun enviarComanda(comanda: com.kenji.rotisseria00.models.Comanda): Pair<Boolean, String?> {
        println("DEBUG: Tentando enviar pedido para $BASE_URL/comandas")
        return try {
            val response = KtorClient.httpClient.post("$BASE_URL/comandas") {
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
                    Pair(false, erroMap["erro"] ?: "Estoque insuficiente para algum item.")
                }
                else -> {
                    Pair(false, "Erro no servidor. Tente novamente.")
                }
            }
        } catch (e: Exception) {
            // Pegamos o nome técnico do erro e a mensagem que o sistema gerou
            val erroReal = "${e.javaClass.simpleName}: ${e.message}"
            Pair(false, "ERRO TÉCNICO:\n$erroReal")
        }
    }

    // 2. Garçom fecha a conta (Muda status para A_PAGAR)
    suspend fun fecharConta(mesa: String): Boolean {
        println("DEBUG: Tentando fechar conta da mesa $mesa")
        return try {
            val response = KtorClient.httpClient.put("$BASE_URL/comandas/$mesa/fechar")
            println("DEBUG: Resposta fechar conta: ${response.status}")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            println("DEBUG: Erro ao fechar conta: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // 3. O Caixa (seu pai) busca quem está devendo
    suspend fun buscarContasPendentes(): List<Comanda> {
        return try {
            KtorClient.httpClient.get("$BASE_URL/caixa/pendentes").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

//    // 4. O Caixa confirma que recebeu o dinheiro (Muda status para PAGO)
//    suspend fun confirmarPagamento(mesa: String): Boolean {
//        return try {
//            val response = KtorClient.httpClient.put("$BASE_URL/caixa/$mesa/pagar")
//            response.status == HttpStatusCode.OK
//        } catch (e: Exception) {
//            e.printStackTrace()
//            false
//        }
//    }

    // 1. Busca todo o estoque
    suspend fun buscarEstoque(): List<com.kenji.rotisseria00.models.ItemEstoqueResponse> {
        return try {
            KtorClient.httpClient.get("$BASE_URL/estoque").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 2. Adiciona um item novo
    suspend fun adicionarEstoque(item: com.kenji.rotisseria00.models.ItemEstoqueRequest): Boolean {
        return try {
            val response = KtorClient.httpClient.post("$BASE_URL/estoque") {
                contentType(ContentType.Application.Json)
                setBody(item)
            }
            response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 3. Atualiza um item existente (O botão de Ajustar Estoque)
    suspend fun atualizarEstoque(id: String, item: com.kenji.rotisseria00.models.ItemEstoqueRequest): Boolean {
        return try {
            val response = KtorClient.httpClient.put("$BASE_URL/estoque/$id") {
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
    // ROTAS DO CARDÁPIO
    // ==========================================

    suspend fun buscarCardapio(): List<com.kenji.rotisseria00.models.ItemCardapioResponse> {
        return try {
            KtorClient.httpClient.get("$BASE_URL/cardapio").body()
        } catch (e: Exception) {
            // Se der erro ao buscar o cardápio, a gente força o crash ou mostra o erro
            e.printStackTrace()
            emptyList()
            // Dica: se o cardápio não carregar, o problema é aqui.
            // O erro mais comum é SerializationException (JSON não bate com o Kotlin).
        }
    }

    suspend fun adicionarPrato(item: com.kenji.rotisseria00.models.ItemCardapioRequest): Boolean {
        return try {
            val response = KtorClient.httpClient.post("$BASE_URL/cardapio") {
                contentType(ContentType.Application.Json)
                setBody(item)
            }
            response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun atualizarPrato(id: String, item: com.kenji.rotisseria00.models.ItemCardapioRequest): Boolean {
        return try {
            val response = KtorClient.httpClient.put("$BASE_URL/cardapio/$id") {
                contentType(ContentType.Application.Json)
                setBody(item)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun buscarContasAbertas(): List<Comanda> {
        return try {
            KtorClient.httpClient.get("$BASE_URL/caixa/abertas").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }



    @kotlinx.serialization.Serializable
    data class PagamentoRequest(
        val metodoPagamento: String,
        val valorFinal: Double? = null,
        val nomeCliente: String? = null // <- NOME CORRIGIDO E COM "? = null"
    )

    // Atualize a função na RotisseriaApi.kt:
    suspend fun confirmarPagamento(mesa: String, metodo: String, valorFinal: Double, nomeFiado: String? = null): Boolean {
        return try {
            val request = com.kenji.rotisseria00.models.PagamentoRequest(
                metodoPagamento = metodo,
                valorFinal = valorFinal,
                nomeCliente = nomeFiado // <- Agora os nomes batem perfeitamente!
            )
            val response = KtorClient.httpClient.put("$BASE_URL/caixa/pagar/$mesa") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    // 2. A tela de Histórico busca tudo que já foi pago
    suspend fun buscarHistorico(): List<com.kenji.rotisseria00.models.Comanda> {
        return try {
            KtorClient.httpClient.get("$BASE_URL/historico").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun buscarFiados(): List<com.kenji.rotisseria00.models.Comanda> {
        return try {
            KtorClient.httpClient.get("$BASE_URL/fiados").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Cliente veio pagar a dívida do Fiado
    suspend fun quitarFiado(mesa: String, metodo: String): Boolean {
        return try {
            // Adicionando explicitamente o valorFinal = null e nomeCliente = null
            val request = com.kenji.rotisseria00.models.PagamentoRequest(
                metodoPagamento = metodo,
                valorFinal = null,
                nomeCliente = null
            )
            val response = KtorClient.httpClient.put("$BASE_URL/fiados/quitar/$mesa") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun buscarResumoDashboard(): com.kenji.rotisseria00.models.DashboardResumo? {
        return try {
            KtorClient.httpClient.get("$BASE_URL/dashboard/resumo").body()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // ROTAS DA COZINHA
    // ==========================================

    suspend fun buscarPedidosCozinha(): List<Comanda> {
        return try {
            KtorClient.httpClient.get("$BASE_URL/cozinha/pedidos").body()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun concluirPedidoCozinha(mesa: String): Boolean {
        return try {
            val response = KtorClient.httpClient.put("$BASE_URL/cozinha/concluir/$mesa")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun cancelarItem(mesa: String, nomeItem: String): Boolean {
        return try {
            val response = KtorClient.httpClient.delete("$BASE_URL/comandas/$mesa/item/$nomeItem")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}