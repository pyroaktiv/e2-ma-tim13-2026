package rs.tim13.slagalica.chat.data.dto

/** Stavka liste konverzacija sa `/api/chat/conversations` (spec 8). */
data class ConversationDto(
    val userId: Int,
    val username: String,
    val avatar: String,
    val lastMessage: String?,
    val lastMessageMine: Boolean,
    val lastMessageAt: String,
    val unreadCount: Int,
    val isOnline: Boolean
)

/** Korisnik iz istog regiona, rezultat regex pretrage na `/api/chat/search`. */
data class ChatUserDto(
    val id: Int,
    val username: String,
    val avatar: String
)

data class ChatHistoryResponseDto(
    val user: ChatUserDto,
    val messages: List<ChatMessageDto>
)

data class ChatMessageDto(
    val id: Int,
    val fromUserId: Int,
    val toUserId: Int,
    val body: String,
    val createdAt: String
)
