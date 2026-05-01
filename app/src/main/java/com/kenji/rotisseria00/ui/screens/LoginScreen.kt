package com.kenji.rotisseria00.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenji.rotisseria00.R
import com.kenji.rotisseria00.models.LoginRequest
import com.kenji.rotisseria00.models.LoginResponse
import com.kenji.rotisseria00.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSucesso: (String) -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    // Novas variáveis para controlar o carregamento e erros
    var isLoading by remember { mutableStateOf(false) }
    var erroMensagem by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope() // Permite rodar código em 2º plano

    val corFundo = Color(0xFF432F17)
    val corBotao = Color(0xFFF8CE6A)
    val corTextoEscuro = Color(0xFFFFFFFF)
    val corTextoClaro = Color(0xFFFFFFFF)

    Column(
        modifier = Modifier.fillMaxSize().background(corFundo).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Image(
            painter = painterResource(id = R.drawable.rotisseriaaaa_removebg_preview),
            contentDescription = "Logo da Rotisseria",
            modifier = Modifier.size(200.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Sistema Rotisseria", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = corTextoClaro)
        Text(text = "Faça login para continuar", style = MaterialTheme.typography.bodyMedium, color = corTextoClaro.copy(alpha = 0.8f))

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Usuário", color = corTextoClaro) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro,
                focusedBorderColor = corBotao, unfocusedBorderColor = corTextoClaro.copy(alpha = 0.5f),
                cursorColor = corBotao
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha", color = corTextoClaro) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = corTextoClaro, unfocusedTextColor = corTextoClaro,
                focusedBorderColor = corBotao, unfocusedBorderColor = corTextoClaro.copy(alpha = 0.5f),
                cursorColor = corBotao
            )
        )

        // Exibe erro se houver
        if (erroMensagem.isNotEmpty()) {
            Text(text = erroMensagem, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (usuario.isBlank() || senha.isBlank()) {
                    erroMensagem = "Preencha todos os campos"
                    return@Button
                }

                // Iniciando a requisição para o servidor!
                coroutineScope.launch {
                    isLoading = true
                    erroMensagem = ""
                    try {
                        // Faz o POST para o Ktor (10.0.2.2 é o localhost do emulador)
                        val response = KtorClient.httpClient.post("https://manfully-tentiest-britt.ngrok-free.dev/login") {
                            contentType(ContentType.Application.Json)
                            setBody(LoginRequest(usuario, senha))
                        }

                        val loginResponse: LoginResponse = response.body()

                        if (loginResponse.sucesso) {
                            // Sucesso! Avisa a navegação passando o perfil que veio do banco
                            onLoginSucesso(loginResponse.perfil ?: "SALAO")
                        } else {
                            erroMensagem = loginResponse.mensagem
                        }
                    } catch (e: Exception) {
                        erroMensagem = "Erro ao conectar no servidor."
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = corBotao, contentColor = corTextoEscuro),
            enabled = !isLoading // Desativa o botão enquanto carrega
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = corTextoEscuro, modifier = Modifier.size(24.dp))
            } else {
                Text("Entrar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}