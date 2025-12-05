package com.example.osvacarry

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.osvacarry.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnTestLogin.setOnClickListener {
            performLogin()
        }

        binding.btnGetChats.setOnClickListener {
            getChats()
        }
    }

    private fun performLogin() {
        log("Iniciando login...")
        lifecycleScope.launch {
            try {
                // Using exact email/pass from prompt
                val request = LoginRequest("admin@hotmail.com", "admin")
                val response = RetrofitClient.api.login(request)
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        currentToken = loginResponse.data.accessToken
                        log("Login EXITOSO!\nToken: $currentToken")
                    } else {
                        log("Login falló: Respuesta vacía")
                    }
                } else {
                    log("Login Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                log("Excepción Login: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun getChats() {
        val token = currentToken
        if (token == null) {
            log("Error: Primero debes loguearte para obtener el token.")
            return
        }

        log("Obteniendo chats...")
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getChats("Bearer $token")
                
                if (response.isSuccessful) {
                    val chatsResponse = response.body()
                    val chatSummaries = chatsResponse?.data

                    log("Conversaciones obtenidas: ${chatSummaries?.size ?: 0}")

                    if (!chatSummaries.isNullOrEmpty()) {
                        val firstChatId = chatSummaries[0].id
                        log("Obteniendo mensajes para la conversación con ID: $firstChatId")

                        val messagesResponse = RetrofitClient.api.getMessages("Bearer $token", firstChatId)

                        if (messagesResponse.isSuccessful) {
                            val messages = messagesResponse.body()?.data
                            log("Mensajes en la conversación '$firstChatId': ${messages?.size ?: 0}")
                            messages?.forEachIndexed { index, message ->
                                log("Mensaje ${index + 1}: ${message.content} (de ${message.senderId})")
                            }
                        } else {
                            log("Error al obtener mensajes: ${messagesResponse.code()} - ${messagesResponse.errorBody()?.string()}")
                        }
                    } else {
                        log("No se encontraron conversaciones.")
                    }
                } else {
                    log("GetChats Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                log("Excepción GetChats: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun log(message: String) {
        Log.d("TEST_APP", message)
        val currentText = binding.tvResult.text.toString()
        binding.tvResult.text = "$message\n\n$currentText"
    }
}
