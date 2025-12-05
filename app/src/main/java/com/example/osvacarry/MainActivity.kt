package com.example.osvacarry

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.osvacarry.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Actividad principal de la aplicación.
 * Sirve como una aplicación de prueba simple para verificar la conectividad con un backend Flask
 * realizando peticiones de login y obtención de chats/mensajes.
 * Utiliza ViewBinding para la UI, Retrofit para las peticiones HTTP y Coroutines para la asincronía.
 */
class MainActivity : ComponentActivity() {

    // Instancia de ViewBinding para acceder a los elementos de la UI
    private lateinit var binding: ActivityMainBinding
    // Variable para almacenar el token de acceso obtenido tras un login exitoso
    private var currentToken: String? = null

    /**
     * Se llama cuando la actividad se crea por primera vez.
     * Aquí se inicializa la UI y se configuran los listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla el layout usando ViewBinding y establece el contenido de la vista
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura los listeners para los botones
        setupListeners()
    }

    /**
     * Configura los OnClickListener para los botones de la UI.
     */
    private fun setupListeners() {
        binding.btnTestLogin.setOnClickListener {
            performLogin()
        }

        binding.btnGetChats.setOnClickListener {
            getChats()
        }
    }

    /**
     * Realiza una petición POST al endpoint de login del backend.
     * Muestra el resultado (token o error) en la UI y en Logcat.
     */
    private fun performLogin() {
        log("Iniciando login...")
        // Lanza una coroutine en el scope del ciclo de vida de la actividad
        lifecycleScope.launch {
            try {
                // Crea el objeto de petición con las credenciales predefinidas
                val request = LoginRequest("admin@hotmail.com", "admin")
                // Realiza la llamada a la API de login
                val response = RetrofitClient.api.login(request)
                
                // Verifica si la petición fue exitosa (código 2xx)
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse != null) {
                        // Almacena el token de acceso
                        currentToken = loginResponse.data.accessToken
                        log("Login EXITOSO!\nToken: $currentToken")
                    } else {
                        log("Login falló: Respuesta vacía")
                    }
                } else {
                    // Muestra el código de error y el cuerpo del error si la petición no fue exitosa
                    log("Login Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                // Captura y registra cualquier excepción de red o de otro tipo
                log("Excepción Login: ${e.message}")
                e.printStackTrace() // Imprime el stack trace para depuración
            }
        }
    }

    /**
     * Realiza una petición GET al endpoint /chats/ para obtener los resúmenes de conversaciones.
     * Si encuentra conversaciones, toma el ID de la primera para luego obtener sus mensajes.
     * Muestra el resultado (cantidad de conversaciones/mensajes o error) en la UI y en Logcat.
     */
    private fun getChats() {
        val token = currentToken
        // Verifica si se ha obtenido un token previamente
        if (token == null) {
            log("Error: Primero debes loguearte para obtener el token.")
            return
        }

        log("Obteniendo chats...")
        // Lanza una coroutine
        lifecycleScope.launch {
            try {
                // Realiza la petición para obtener los resúmenes de chats
                val response = RetrofitClient.api.getChats("Bearer $token")
                
                if (response.isSuccessful) {
                    val chatsResponse = response.body()
                    val chatSummaries = chatsResponse?.data

                    log("Conversaciones obtenidas: ${chatSummaries?.size ?: 0}")

                    // Si se encontraron conversaciones, procede a obtener los mensajes de la primera
                    if (!chatSummaries.isNullOrEmpty()) {
                        val firstChatId = chatSummaries[0].id
                        log("Obteniendo mensajes para la conversación con ID: $firstChatId")

                        // Realiza la petición para obtener los mensajes de la conversación específica
                        val messagesResponse = RetrofitClient.api.getMessages("Bearer $token", firstChatId)

                        if (messagesResponse.isSuccessful) {
                            val messages = messagesResponse.body()?.data
                            log("Mensajes en la conversación '$firstChatId': ${messages?.size ?: 0}")
                            // Itera y muestra el contenido de cada mensaje
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

    /**
     * Función auxiliar para mostrar mensajes en el TextView de la UI y en Logcat.
     * @param message El mensaje a mostrar.
     */
    private fun log(message: String) {
        Log.d("TEST_APP", message) // Log a Logcat
        val currentText = binding.tvResult.text.toString()
        // Agrega el nuevo mensaje al principio del TextView
        binding.tvResult.text = "$message\n\n$currentText"
    }
}