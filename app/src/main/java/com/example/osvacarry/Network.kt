package com.example.osvacarry

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

// --- Modelos de Datos para Peticiones y Respuestas de la API ---

/**
 * Representa el cuerpo de la petición de login.
 * Contiene el email y la contraseña del usuario.
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Representa la sección 'data' dentro de la respuesta de login exitosa.
 * Contiene el token de acceso y el tipo de token.
 */
data class LoginData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

/**
 * Representa la respuesta completa de una petición de login.
 * Envuelve los datos de login en un objeto 'data'.
 */
data class LoginResponse(
    val data: LoginData
)

// --- Modelos de Datos para el Resumen de Chats (GET /chats/) ---

/**
 * Representa un resumen de una conversación de chat.
 * Incluye información del chat, el otro usuario, el último mensaje y mensajes no leídos.
 */
data class ChatSummary(
    val id: String,
    @SerializedName("other_user") val otherUser: OtherUser,
    @SerializedName("last_message") val lastMessage: LastMessage?, // Puede ser nulo si no hay mensajes
    @SerializedName("unread_messages") val unreadMessages: Int
)

/**
 * Representa los datos del otro usuario en un resumen de chat.
 */
data class OtherUser(
    val id: String,
    val name: String,
    @SerializedName("avatar_url") val avatarUrl: String? // Puede ser nulo
)

/**
 * Representa la información del último mensaje en un resumen de chat.
 */
data class LastMessage(
    val content: String,
    val timestamp: String
)

/**
 * Representa la respuesta completa del endpoint GET /chats/.
 * Contiene una lista de resúmenes de conversaciones.
 */
data class ChatsResponse(
    val data: List<ChatSummary>
)

// --- Modelos de Datos para Mensajes de un Chat Específico (GET /chats/{chatId}) ---

/**
 * Representa un mensaje individual dentro de una conversación.
 */
data class Message(
    val id: String,
    @SerializedName("sender_id") val senderId: String,
    val content: String,
    val timestamp: String,
    val delivered: Boolean,
    @SerializedName("conversation_id") val conversationId: String
)

/**
 * Representa la respuesta completa del endpoint GET /chats/{chatId}.
 * Contiene una lista de mensajes para una conversación específica.
 */
data class MessagesResponse(
    val data: List<Message>
)

// --- Interfaz de Servicio de API ---

/**
 * Define los endpoints de la API y sus métodos HTTP.
 * Utiliza suspend functions para operaciones asíncronas con Coroutines.
 */
interface ApiService {
    /**
     * Realiza una petición POST para iniciar sesión.
     * @param request El objeto LoginRequest que contiene las credenciales.
     * @return Una respuesta de Retrofit que contiene LoginResponse.
     */
    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /**
     * Realiza una petición GET para obtener un resumen de todas las conversaciones de chat del usuario.
     * @param token El token de autorización Bearer.
     * @return Una respuesta de Retrofit que contiene ChatsResponse.
     */
    @GET("chats/")
    suspend fun getChats(@Header("Authorization") token: String): Response<ChatsResponse>

    /**
     * Realiza una petición GET para obtener todos los mensajes de una conversación específica.
     * @param token El token de autorización Bearer.
     * @param chatId El ID de la conversación de la cual se quieren obtener los mensajes.
     * @return Una respuesta de Retrofit que contiene MessagesResponse.
     */
    @GET("chats/{chatId}")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String
    ): Response<MessagesResponse>
}

// --- Cliente Retrofit para Configuración de la API ---

/**
 * Objeto Singleton que proporciona una instancia configurada de Retrofit y ApiService.
 */
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:5000/" // URL base de tu backend Flask

    // Interceptor para loggear las peticiones y respuestas HTTP en Logcat
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Establece el nivel de log a BODY para ver los cuerpos completos de request y response
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Cliente OkHttpClient configurado con el interceptor de logging
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Instancia perezosa de ApiService, se inicializa la primera vez que se accede
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // Establece la URL base
            .addConverterFactory(GsonConverterFactory.create()) // Añade el conversor de Gson para JSON
            .client(okHttpClient) // Asocia el cliente HTTP personalizado con el interceptor
            .build() // Construye la instancia de Retrofit
            .create(ApiService::class.java) // Crea una implementación de la interfaz ApiService
    }
}