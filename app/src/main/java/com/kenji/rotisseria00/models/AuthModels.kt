package com.kenji.rotisseria00.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val usuario: String,
    val senha: String
)

@Serializable
data class LoginResponse(
    val sucesso: Boolean,
    val perfil: String? = null,
    val mensagem: String
)