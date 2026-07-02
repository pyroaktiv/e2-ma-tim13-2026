package rs.tim13.slagalica.chat.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rs.tim13.slagalica.chat.data.api.ChatApiService
import rs.tim13.slagalica.chat.data.dto.ConversationDto
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.network.socket.ServerMessage

class ChatListViewModel : ViewModel() {

    private val _conversations = MutableLiveData<List<ConversationDto>>(emptyList())
    val conversations: LiveData<List<ConversationDto>> = _conversations

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun refresh(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            val result = runCatching {
                val api = RetrofitClient.getClient(context).create(ChatApiService::class.java)
                api.getConversations()
            }.getOrNull()
            if (result != null && result.isSuccessful) {
                _conversations.value = result.body().orEmpty()
            }
            _loading.value = false
        }
    }

    /** Novodošla poruka iz drugog konteksta (npr. dok je lista otvorena) — povuče listu iznova. */
    fun onServerMessage(message: ServerMessage, context: Context) {
        if (message is ServerMessage.ChatMessage) refresh(context)
    }
}
