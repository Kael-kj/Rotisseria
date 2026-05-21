package com

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import com.database.MongoConfig
import com.models.*
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates.*
import com.database.MongoConfig.cardapioCollection
import com.database.MongoConfig.comandasCollection
import com.database.MongoConfig.estoqueCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable


fun Application.configureRouting() {
    routing {

        // ==========================================
        // ROTAS DA COMANDA E DO SALÃO
        // ==========================================

        post("/comandas") {
            try {
                val novaComanda = call.receive<Comanda>()
                val itensSemEstoque = mutableListOf<String>()

                println("--- [NOVO PEDIDO] VERIFICANDO ESTOQUE ---")

                // 1. O ÁRBITRO: Confere o estoque real no banco ANTES de aprovar
                novaComanda.itens.forEach { itemVendido ->
                    val pratoBanco = cardapioCollection.find(eq("nome", itemVendido.nome)).firstOrNull()

                    if (pratoBanco != null && pratoBanco.estoqueAtual != null) {
                        println("-> Prato: ${pratoBanco.nome} | Pedido: ${itemVendido.quantidade} | Restam: ${pratoBanco.estoqueAtual}")

                        // Se o que o cliente pediu for maior do que tem na panela, barra o item!
                        if (pratoBanco.estoqueAtual < itemVendido.quantidade) {
                            itensSemEstoque.add("${itemVendido.nome} (Restam: ${pratoBanco.estoqueAtual})")
                        }
                    }
                }

                // Se faltou estoque de QUALQUER item, derruba a comanda inteira, avisa o App e não desconta nada.
                if (itensSemEstoque.isNotEmpty()) {
                    println("❌ BLOQUEADO: Estoque insuficiente para ${itensSemEstoque.joinToString(", ")}")
                    call.respond(
                        HttpStatusCode.Conflict, // Código de Conflito! Isso aciona o seu Pop-up no App
                        mapOf("erro" to "Estoque insuficiente para: ${itensSemEstoque.joinToString(", ")}")
                    )
                    return@post // Para a execução do código aqui.
                }

                // 2. TUDO CERTO! Salva a comanda no banco e debita o estoque
                println("✅ ESTOQUE OK: Salvando pedido e debitando ingredientes...")
                comandasCollection.insertOne(novaComanda)

                novaComanda.itens.forEach { itemVendido ->
                    // Debita a quantidade do prato em si
                    cardapioCollection.updateOne(
                        and(eq("nome", itemVendido.nome), ne("estoqueAtual", null)),
                        inc("estoqueAtual", -itemVendido.quantidade)
                    )

                    // Debita os ingredientes individuais (Ficha técnica)
                    val pratoCompleto = cardapioCollection.find(eq("nome", itemVendido.nome)).firstOrNull()
                    pratoCompleto?.ingredientes?.forEach { ingrediente ->
                        val totalADescontar = ingrediente.quantidadeNecessaria * itemVendido.quantidade
                        estoqueCollection.updateOne(
                            eq("nome", ingrediente.nomeIngrediente),
                            inc("quantidadeAtual", -totalADescontar)
                        )
                    }
                }

                call.respond(HttpStatusCode.Created, mapOf("mensagem" to "Pedido recebido com sucesso!"))

            } catch (e: Exception) {
                println("❌ ERRO INTERNO: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to "Erro ao processar pedido: ${e.message}"))
            }
        }

        put("/comandas/{mesa}/fechar") {
            try {
                val numeroMesa = call.parameters["mesa"] ?: return@put call.respond(HttpStatusCode.BadRequest)

                // Filtro: Seleciona tudo o que estiver aberto nesta mesa
                val filtro = and(eq("mesa", numeroMesa), eq("statusComanda", "EM_ABERTO"))

                // CORREÇÃO: usamos updateMany para mover todos os itens da mesa para o caixa
                val resultado = comandasCollection.updateMany(filtro, set("statusComanda", "A_PAGAR"))

                if (resultado.matchedCount > 0L) {
                    call.respond(HttpStatusCode.OK, mapOf("mensagem" to "Conta enviada ao caixa!"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("erro" to "Nenhuma conta aberta nesta mesa."))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to e.message))
            }
        }

        // ==========================================
        // ROTAS DO CAIXA
        // ==========================================

        get("/caixa/pendentes") {
            try {
                val contasPendentes = comandasCollection.find(eq("statusComanda", "A_PAGAR")).toList()
                val resposta = contasPendentes.map { comanda ->
                    ComandaResponse(comanda.mesa, comanda.nomeCliente, comanda.itens, comanda.total, comanda.statusComanda)
                }
                call.respond(HttpStatusCode.OK, resposta)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to e.message))
            }
        }

        put("/caixa/pagar/{mesa}") {
            val mesaStr = call.parameters["mesa"] ?: ""
            try {
                val request = call.receive<PagamentoRequest>()
                val dataAtual = java.text.SimpleDateFormat("dd/MM/yyyy 'às' HH:mm").format(java.util.Date())
                val statusFinal = if (request.metodoPagamento == "FIADO") "FIADO" else "PAGO"

                val filtro = and(
                    eq("mesa", mesaStr),
                    or(eq("statusComanda", "A_PAGAR"), eq("statusComanda", "EM_ABERTO"))
                )

                val updates = combine(
                    set("statusComanda", statusFinal),
                    set("metodoPagamento", request.metodoPagamento),
                    set("dataFechamento", dataAtual),
                    set("nomeCliente", request.nomeCliente ?: "CLIENTE")
                )

                // CORREÇÃO: updateMany para garantir que todos os pedidos da mesa virem "PAGO"
                val resultado = comandasCollection.updateMany(filtro, updates)

                if (resultado.matchedCount > 0L) {
                    call.respond(HttpStatusCode.OK, mapOf("mensagem" to "Pagamento registrado!"))
                } else {
                    call.respond(HttpStatusCode.NotFound, "Pedido não encontrado.")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

        get("/caixa/abertas") {
            try {
                // Busca EM_ABERTO (balcão) e A_PAGAR (mesas)
                val abertas = comandasCollection.find(
                    `in`("statusComanda", listOf("EM_ABERTO", "A_PAGAR"))
                ).toList()
                call.respond(HttpStatusCode.OK, abertas)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

        // ==========================================
        // SISTEMA (LOGIN E SETUP)
        // ==========================================

        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                val usuarioEncontrado = MongoConfig.usuariosCollection.find(eq("usuario", request.usuario)).firstOrNull()

                if (usuarioEncontrado != null && usuarioEncontrado.senha == request.senha) {
                    call.respond(HttpStatusCode.OK, LoginResponse(true, usuarioEncontrado.perfil, "Login aprovado!"))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, LoginResponse(false, null, "Usuário ou senha incorretos."))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, LoginResponse(false, null, "Erro: ${e.message}"))
            }
        }

        get("/setup") {
            try {
                MongoConfig.usuariosCollection.insertOne(Usuario(null, "admin", "123", "ADMIN"))
                MongoConfig.usuariosCollection.insertOne(Usuario(null, "salao", "123", "SALAO"))
                MongoConfig.usuariosCollection.insertOne(Usuario(null, "cozinha", "123", "COZINHA"))
                call.respond(HttpStatusCode.Created, "Usuários criados!")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

        // ==========================================
        // ESTOQUE E CARDÁPIO
        // ==========================================

        get("/estoque") {
            try {
                val itens = estoqueCollection.find().toList().map {
                    ItemEstoqueResponse(it.id?.toHexString() ?: "", it.nome, it.quantidadeAtual, it.quantidadeMinima, it.unidade, it.ultimaCompra)
                }
                call.respond(HttpStatusCode.OK, itens)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }
        get("/cardapio") {
            try {
                val pratos = cardapioCollection.find().toList().map {
                    ItemCardapioResponse(
                        id = it.id?.toHexString() ?: "",
                        nome = it.nome,
                        categoria = it.categoria,
                        preco = it.preco,
                        disponivel = it.disponivel,
                        parceria = it.parceria,
                        limiteDiario = it.limiteDiario,
                        estoqueAtual = it.estoqueAtual,
                        ingredientes = it.ingredientes
                    )
                }
                call.respond(HttpStatusCode.OK, pratos)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to e.message))
            }
        }

        post("/cardapio") {
            try {
                val request = call.receive<ItemCardapioRequest>()
                val novoPrato = ItemCardapio(
                    id = null,
                    nome = request.nome,
                    categoria = request.categoria,
                    preco = request.preco,
                    disponivel = request.disponivel,
                    parceria = request.parceria,
                    limiteDiario = request.limiteDiario,
                    estoqueAtual = request.estoqueAtual,
                    ingredientes = request.ingredientes
                )
                cardapioCollection.insertOne(novoPrato)
                call.respond(HttpStatusCode.Created, mapOf("mensagem" to "Prato cadastrado com sucesso!"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

// 2. Rota para atualizar um prato existente (Editar)
        put("/cardapio/{id}") {
            try {
                val idStr = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "ID ausente")
                val request = call.receive<ItemCardapioRequest>()

                println("--- LOG SERVIDOR: ATUALIZANDO PRATO ---")
                println("ID recebido: $idStr")
                println("Novo Status Disponível: ${request.disponivel}")

                // Converte para ObjectId com segurança
                val objId = try {
                    org.bson.types.ObjectId(idStr)
                } catch (e: Exception) {
                    return@put call.respond(HttpStatusCode.BadRequest, "ID em formato inválido")
                }

                val filtro = eq("_id", objId)

                // Monta a atualização garantindo que todos os campos sejam salvos
                val atualizacao = combine(
                    set("nome", request.nome),
                    set("categoria", request.categoria),
                    set("preco", request.preco),
                    set("disponivel", request.disponivel),
                    set("parceria", request.parceria),
                    set("limiteDiario", request.limiteDiario),
                    set("estoqueAtual", request.estoqueAtual),
                    set("ingredientes", request.ingredientes)
                )

                val resultado = cardapioCollection.updateOne(filtro, atualizacao)

                if (resultado.matchedCount > 0L) {
                    println("Sucesso: Prato atualizado no banco.")
                    call.respond(HttpStatusCode.OK, mapOf("mensagem" to "Prato atualizado!"))
                } else {
                    println("Aviso: Prato com ID $idStr não encontrado no banco.")
                    call.respond(HttpStatusCode.NotFound, "Prato não encontrado.")
                }
            } catch (e: Exception) {
                println("ERRO CRÍTICO NO KTOR: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro desconhecido")
            }
        }

        // ==========================================
        // HISTÓRICO E FIADOS
        // ==========================================

        get("/historico") {
            try {
                val historico = comandasCollection.find(`in`("statusComanda", listOf("PAGO", "FIADO"))).toList().map {
                    ComandaResponse(it.mesa, it.nomeCliente, it.itens, it.total, it.statusComanda, it.metodoPagamento, it.dataFechamento)
                }
                call.respond(HttpStatusCode.OK, historico)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

        get("/fiados") {
            try {
                val fiados = comandasCollection.find(eq("statusComanda", "FIADO")).toList().map {
                    ComandaResponse(it.mesa, it.nomeCliente, it.itens, it.total, it.statusComanda, it.metodoPagamento, it.dataFechamento)
                }
                call.respond(HttpStatusCode.OK, fiados)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

        // ==========================================
        // QUITAR DÍVIDA DE FIADO (ADICIONE ISTO)
        // ==========================================
        put("/fiados/quitar/{mesa}") {
            try {
                val mesaStr = call.parameters["mesa"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<PagamentoRequest>()
                val dataAtual = java.text.SimpleDateFormat("dd/MM/yyyy 'às' HH:mm").format(java.util.Date())

                // Filtro: Procura a comanda exata que está com status FIADO
                val filtro = and(eq("mesa", mesaStr), eq("statusComanda", "FIADO"))

                // Atualização: Transforma em PAGO e registra a data da quitação
                val dadosAtualizacao = org.bson.Document("\$set", org.bson.Document("statusComanda", "PAGO")
                    .append("metodoPagamento", request.metodoPagamento)
                    .append("dataFechamento", "$dataAtual (Dívida Quitada)")
                )

                val resultado = comandasCollection.updateOne(filtro, dadosAtualizacao)

                if (resultado.matchedCount > 0L) {
                    call.respond(HttpStatusCode.OK, mapOf("mensagem" to "Dívida quitada com sucesso!"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("erro" to "Registro de fiado não encontrado."))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to e.message))
            }
        }

        // 1. Nova rota para buscar fiados AGRUPADOS por cliente
        get("/fiados/agrupados") {
            try {
                val todosFiados = comandasCollection.find(eq("statusComanda", "FIADO")).toList()

                // Agrupa por nome do cliente e soma os totais
                val agrupado = todosFiados.groupBy { it.nomeCliente.uppercase() }
                    .map { (nome, comandas) ->
                        mapOf(
                            "cliente" to nome,
                            "totalDevedor" to comandas.sumOf { it.total },
                            "quantidadePedidos" to comandas.size,
                            "pedidos" to comandas // Lista detalhada para ver o que ele comeu
                        )
                    }
                call.respond(HttpStatusCode.OK, agrupado)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

// 2. Nova rota para quitar TUDO de um cliente
        put("/fiados/quitar-todos/{nomeCliente}") {
            try {
                val nome = call.parameters["nomeCliente"] ?: ""
                val request = call.receive<PagamentoRequest>()
                val dataHoje = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())

                // Filtra tudo o que for FIADO deste cliente (independente de maiúsculas/minúsculas)
                val filtro = and(
                    regex("nomeCliente", "^$nome$", "i"),
                    eq("statusComanda", "FIADO")
                )

                val atualizacao = combine(
                    set("statusComanda", "PAGO"),
                    set("metodoPagamento", request.metodoPagamento),
                    set("dataFechamento", "$dataHoje (Quitado)")
                )

                val resultado = comandasCollection.updateMany(filtro, atualizacao)
                call.respond(HttpStatusCode.OK, mapOf("mensagem" to "Quitado!"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Erro")
            }
        }

        // ==========================================
        // DASHBOARD (O CORRETO PARA DESTRAVAR)
        // ==========================================

        get("/dashboard/resumo") {
            try {
                val dataHoje = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())

                val vendasDeHoje = comandasCollection.find(
                    and(eq("statusComanda", "PAGO"), regex("dataFechamento", "^$dataHoje"))
                ).toList()

                val vHoje = vendasDeHoje.sumOf { it.total }
                val pHoje = vendasDeHoje.size
                val tMedio = if (pHoje > 0) vHoje / pHoje else 0.0

                val mAbertas = comandasCollection.countDocuments(eq("statusComanda", "EM_ABERTO")).toInt()
                val aPagamento = comandasCollection.countDocuments(eq("statusComanda", "A_PAGAR")).toInt()

                val fiadosBanco = comandasCollection.find(eq("statusComanda", "FIADO")).toList()
                val qFiados = fiadosBanco.size
                val vFiados = fiadosBanco.sumOf { it.total }

                val estoqueCompleto = estoqueCollection.find().toList()
                val eBaixo = estoqueCompleto.count { it.quantidadeAtual <= it.quantidadeMinima }

                // AJUSTADO: Usando o nome exato 'estoqueBaixo' que o app exige
                val resumo = DashboardResumo(
                    vendasHoje = vHoje,
                    pedidosHoje = pHoje,
                    ticketMedio = tMedio,
                    estoqueBaixo = eBaixo,
                    mesasAbertas = mAbertas,
                    aguardandoPagamento = aPagamento,
                    qtdFiados = qFiados,
                    valorFiados = vFiados
                )

                call.respond(HttpStatusCode.OK, resumo)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to e.message))
            }
        }

        // ==========================================
        // ROTAS DA COZINHA
        // ==========================================

        get("/cozinha/pedidos") {
            try {
                // 1. Busca todas as comandas que os clientes ainda não pagaram (EM_ABERTO)
                val abertas = comandasCollection
                    .find(com.mongodb.client.model.Filters.eq("statusComanda", "EM_ABERTO"))
                    .toList()

                // 2. A MÁGICA DA OPÇÃO B: Filtra as comandas para a cozinha
                val pedidosPendentes = abertas.filter { comanda ->
                    // Só mantém a comanda na tela se tiver algum item que NÃO está "PRONTO"
                    comanda.itens.any { it.statusCozinha != "PRONTO" }
                }

                // 3. Monta a resposta limpa apenas com os pedidos que a cozinha precisa fazer
                val resposta = pedidosPendentes.map { comanda ->
                    ComandaResponse(
                        mesa = comanda.mesa,
                        nomeCliente = comanda.nomeCliente,
                        itens = comanda.itens,
                        total = comanda.total,
                        statusComanda = comanda.statusComanda,
                        metodoPagamento = comanda.metodoPagamento,
                        dataFechamento = comanda.dataFechamento,
                        dataEnvioCozinha = comanda.dataEnvioCozinha
                    )
                }

                call.respond(io.ktor.http.HttpStatusCode.OK, resposta)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("erro" to e.message))
            }
        }

        put("/cozinha/concluir/{mesa}") {
            try {
                val mesaStr = call.parameters["mesa"] ?: return@put call.respond(io.ktor.http.HttpStatusCode.BadRequest)

                val filtro = com.mongodb.client.model.Filters.and(
                    com.mongodb.client.model.Filters.eq("mesa", mesaStr),
                    com.mongodb.client.model.Filters.eq("statusComanda", "EM_ABERTO")
                )

                // Atualiza o "statusCozinha" de TODOS os itens daquela mesa para "PRONTO"
                val resultado = comandasCollection.updateOne(
                    filtro,
                    org.bson.Document("\$set", org.bson.Document("itens.$[].statusCozinha", "PRONTO"))
                )

                // MUDANÇA AQUI: Usando matchedCount.
                // Se ele achou a mesa no banco, dá OK (mesmo se já estivesse PRONTO antes)
                if (resultado.matchedCount > 0L) {
                    call.respond(io.ktor.http.HttpStatusCode.OK, mapOf("mensagem" to "Pedido da mesa $mesaStr concluído!"))
                } else {
                    call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("erro" to "Mesa não encontrada ou não está EM_ABERTO."))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("erro" to e.message))
            }
        }
    }
}