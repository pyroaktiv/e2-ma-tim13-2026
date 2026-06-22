package rs.tim13.slagalica.chat.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rs.tim13.slagalica.chat.data.api.ChatApiService
import rs.tim13.slagalica.chat.data.dto.ChatUserDto
import rs.tim13.slagalica.core.network.RetrofitClient

class ChatSearchViewModel : ViewModel() {

    private val _results = MutableLiveData<List<ChatUserDto>>(emptyList())
    val results: LiveData<List<ChatUserDto>> = _results

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun search(context: Context, pattern: String) {
        if (pattern.isBlank()) {
            _results.value = emptyList()
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                val api = RetrofitClient.getClient(context).create(ChatApiService::class.java)
                api.searchUsers(pattern)
            }.getOrNull()
            when {
                result != null && result.isSuccessful -> _results.value = result.body().orEmpty()
                result != null -> _error.value = "Neispravan izraz za pretragu."
                else -> _error.value = "Greška pri pretrazi."
            }
        }
    }
}
