package rs.tim13.slagalica.chat.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rs.tim13.slagalica.chat.data.api.ChatApiService
import rs.tim13.slagalica.chat.data.dto.ChatMessageDto
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.network.socket.ClientMessage
import rs.tim13.slagalica.core.network.socket.ServerMessage
import rs.tim13.slagalica.core.network.socket.SocketManager

class ChatConversationViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessageDto>>(emptyList())
    val messages: LiveData<List<ChatMessageDto>> = _messages

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadHistory(context: Context, otherUserId: Int) {
        viewModelScope.launch {
            val result = runCatching {
                val api = RetrofitClient.getClient(context).create(ChatApiService::class.java)
                api.getMessages(otherUserId)
            }.getOrNull()
            if (result != null && result.isSuccessful) {
                _messages.value = result.body()?.messages.orEmpty()
            }
        }
    }

    fun sendMessage(otherUserId: Int, body: String) {
        val text = body.trim()
        if (text.isEmpty()) return
        SocketManager.send(ClientMessage.SendChatMessage(toUserId = otherUserId, body = text))
    }

    fun onServerMessage(message: ServerMessage, otherUserId: Int) {
        when (message) {
            is ServerMessage.ChatMessage -> {
                if (message.fromUserId == otherUserId || message.toUserId == otherUserId) {
                    val incoming = ChatMessageDto(
                        id = message.id,
                        fromUserId = message.fromUserId,
                        toUserId = message.toUserId,
                        body = message.body,
                        createdAt = message.createdAt
                    )
                    _messages.value = (_messages.value.orEmpty() + incoming).distinctBy { it.id }
                }
            }
            is ServerMessage.ServerError -> _error.value = message.message
            else -> Unit
        }
    }

    fun consumeError() {
        _error.value = null
    }
}
