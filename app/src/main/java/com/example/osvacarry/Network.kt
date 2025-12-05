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

// --- Login Models ---
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginData(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class LoginResponse(
    val data: LoginData
)

// --- Chat Summary Models (GET /chats/) ---
data class ChatSummary(
    val id: String,
    @SerializedName("other_user") val otherUser: OtherUser,
    @SerializedName("last_message") val lastMessage: LastMessage?,
    @SerializedName("unread_messages") val unreadMessages: Int
)

data class OtherUser(
    val id: String,
    val name: String,
    @SerializedName("avatar_url") val avatarUrl: String?
)

data class LastMessage(
    val content: String,
    val timestamp: String
)

data class ChatsResponse(
    val data: List<ChatSummary>
)

// --- Message Models (GET /chats/{chatId}) ---
data class Message(
    val id: String,
    @SerializedName("sender_id") val senderId: String,
    val content: String,
    val timestamp: String,
    val delivered: Boolean,
    @SerializedName("conversation_id") val conversationId: String
)

data class MessagesResponse(
    val data: List<Message>
)

// --- API Interface ---
interface ApiService {
    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("chats/")
    suspend fun getChats(@Header("Authorization") token: String): Response<ChatsResponse>

    @GET("chats/{chatId}")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("chatId") chatId: String
    ): Response<MessagesResponse>
}

// --- Retrofit Client ---
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:5000/"

    private val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(ApiService::class.java)
    }
}