package rs.tim13.slagalica.izazov.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.network.socket.ChallengeDto
import rs.tim13.slagalica.core.network.socket.ClientMessage
import rs.tim13.slagalica.core.network.socket.ServerMessage
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.izazov.data.api.ChallengeApiService

sealed class ChallengeListEvent {
    data class Created(val challengeId: String) : ChallengeListEvent()
    data class Error(val message: String) : ChallengeListEvent()
}

class ChallengeListViewModel : ViewModel() {

    private val _challenges = MutableLiveData<List<ChallengeDto>>(emptyList())
    val challenges: LiveData<List<ChallengeDto>> = _challenges

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _events = MutableLiveData<ChallengeListEvent?>()
    val events: LiveData<ChallengeListEvent?> = _events

    fun refresh(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            val result = runCatching {
                val api = RetrofitClient.getClient(context).create(ChallengeApiService::class.java)
                api.listChallenges()
            }.getOrNull()
            if (result != null && result.isSuccessful) {
                _challenges.value = result.body().orEmpty()
            }
            _loading.value = false
        }
    }

    fun createChallenge(stars: Int, tokens: Int) {
        SocketManager.send(ClientMessage.CreateChallenge(stars = stars, tokens = tokens))
    }

    fun onServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.ChallengeCreated -> _events.value = ChallengeListEvent.Created(message.challengeId)
            is ServerMessage.ServerError -> _events.value = ChallengeListEvent.Error(message.message)
            else -> Unit
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}
