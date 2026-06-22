package rs.tim13.slagalica.izazov.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.network.socket.ChallengeDto
import rs.tim13.slagalica.core.network.socket.ClientMessage
import rs.tim13.slagalica.core.network.socket.MatchContentDto
import rs.tim13.slagalica.core.network.socket.ServerMessage
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.profil.data.api.ProfileApiService

sealed class ChallengeLobbyEvent {
    data object Cancelled : ChallengeLobbyEvent()
    data class Started(val content: MatchContentDto) : ChallengeLobbyEvent()
    data class Error(val message: String) : ChallengeLobbyEvent()
}

class ChallengeLobbyViewModel(private val challengeId: String) : ViewModel() {

    private val _challenge = MutableLiveData<ChallengeDto?>(null)
    val challenge: LiveData<ChallengeDto?> = _challenge

    private val _myUserId = MutableLiveData<Int?>(null)
    val myUserId: LiveData<Int?> = _myUserId

    private val _events = MutableLiveData<ChallengeLobbyEvent?>()
    val events: LiveData<ChallengeLobbyEvent?> = _events

    fun fetchProfile(context: Context) {
        viewModelScope.launch {
            val result = runCatching {
                val api = RetrofitClient.getClient(context).create(ProfileApiService::class.java)
                api.getProfile()
            }.getOrNull()
            if (result != null && result.isSuccessful) {
                _myUserId.value = result.body()?.id
            }
        }
    }

    fun join() {
        SocketManager.send(ClientMessage.JoinChallenge(challengeId = challengeId))
    }

    fun start() {
        SocketManager.send(ClientMessage.StartChallenge(challengeId = challengeId))
    }

    fun leave() {
        SocketManager.send(ClientMessage.LeaveChallenge(challengeId = challengeId))
    }

    fun onServerMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.ChallengeUpdate ->
                if (message.challenge.id == challengeId) _challenge.value = message.challenge
            is ServerMessage.ChallengeCancelled ->
                if (message.challengeId == challengeId) _events.value = ChallengeLobbyEvent.Cancelled
            is ServerMessage.ChallengeStarted ->
                if (message.challengeId == challengeId) _events.value = ChallengeLobbyEvent.Started(message.content)
            is ServerMessage.ServerError -> _events.value = ChallengeLobbyEvent.Error(message.message)
            else -> Unit
        }
    }

    fun consumeEvent() {
        _events.value = null
    }
}

class ChallengeLobbyViewModelFactory(private val challengeId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ChallengeLobbyViewModel(challengeId) as T
}
