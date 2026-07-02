package rs.tim13.slagalica.chat.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import rs.tim13.slagalica.chat.data.dto.ChatHistoryResponseDto
import rs.tim13.slagalica.chat.data.dto.ChatUserDto
import rs.tim13.slagalica.chat.data.dto.ConversationDto

interface ChatApiService {
    @GET("/api/chat/conversations")
    suspend fun getConversations(): Response<List<ConversationDto>>

    @GET("/api/chat/messages/{id}")
    suspend fun getMessages(@Path("id") userId: Int): Response<ChatHistoryResponseDto>

    @GET("/api/chat/search")
    suspend fun searchUsers(@Query("q") pattern: String): Response<List<ChatUserDto>>
}
