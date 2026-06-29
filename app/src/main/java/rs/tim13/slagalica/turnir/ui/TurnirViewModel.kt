package rs.tim13.slagalica.turnir.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import rs.tim13.slagalica.core.network.socket.ClientMessage
import rs.tim13.slagalica.core.network.socket.ServerMessage
import rs.tim13.slagalica.core.network.socket.SocketManager

class TurnirViewModel : ViewModel() {

    private val _state = MutableLiveData<TurnirUiState>(TurnirUiState.Idle)
    val state: LiveData<TurnirUiState> = _state

    private val _event = MutableLiveData<TurnirEvent?>(null)
    val event: LiveData<TurnirEvent?> = _event

    var foundMessage: ServerMessage.TournamentFound? = null
    var finalStartedMessage: ServerMessage.TournamentFinalStarted? = null

    private val observer = Observer<ServerMessage> { handleMessage(it) }

    init { SocketManager.incoming.observeForever(observer) }

    override fun onCleared() {
        super.onCleared()
        SocketManager.incoming.removeObserver(observer)
    }

    fun findTournament() {
        SocketManager.send(ClientMessage.FindTournament())
        _state.postValue(TurnirUiState.Searching)
    }

    fun cancelTournament() {
        SocketManager.send(ClientMessage.CancelTournament())
    }

    fun consumeEvent() {
        _event.value = null
    }

    fun reset() {
        foundMessage = null
        finalStartedMessage = null
        _state.value = TurnirUiState.Idle
        _event.value = null
    }

    private fun handleMessage(msg: ServerMessage) = when (msg) {
        is ServerMessage.TournamentQueued -> _state.postValue(TurnirUiState.Queued)
        is ServerMessage.TournamentFound -> {
            foundMessage = msg
            _state.postValue(TurnirUiState.BracketReady(msg))
            _event.postValue(TurnirEvent.NavigateToBracket)
        }
        is ServerMessage.TournamentCancelled -> {
            _state.postValue(TurnirUiState.Idle)
            _event.postValue(TurnirEvent.Cancelled)
        }
        is ServerMessage.TournamentSemiOver -> _state.postValue(TurnirUiState.SemiOver(msg))
        is ServerMessage.TournamentFinalStarted -> {
            finalStartedMessage = msg
            _state.postValue(TurnirUiState.FinalReady(msg))
        }
        is ServerMessage.TournamentUpdate -> _state.postValue(TurnirUiState.FinalOngoing(msg))
        is ServerMessage.TournamentOver -> _state.postValue(TurnirUiState.TournamentComplete(msg))
        else -> Unit
    }
}
