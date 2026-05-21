package com.database

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.models.*

object MongoConfig {
    // Sua string de conexão do Atlas continua a mesma
    private val client = MongoClient.create("mongodb+srv://admin_rotisseria:kL3A7ZeR9vT4jPqI@cluster0.1mgi5zk.mongodb.net/?appName=Cluster0")

    private val database = client.getDatabase("rotisseria_db")

    // Todas as coleções puxando do mesmo lugar:
    val cardapioCollection = database.getCollection<ItemCardapio>("cardapio")
    val estoqueCollection = database.getCollection<ItemEstoque>("estoque")
    val comandasCollection = database.getCollection<Comanda>("comandas")
    val usuariosCollection = database.getCollection<Usuario>("usuarios")
}