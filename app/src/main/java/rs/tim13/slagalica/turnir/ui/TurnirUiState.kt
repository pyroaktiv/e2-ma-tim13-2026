package rs.tim13.slagalica.turnir.ui

import rs.tim13.slagalica.core.network.socket.ServerMessage

sealed class TurnirUiState {
    data object Idle : TurnirUiState()
    data object Searching : TurnirUiState()
    data object Queued : TurnirUiState()
    data class BracketReady(val found: ServerMessage.TournamentFound) : TurnirUiState()
    data class SemiOver(val msg: ServerMessage.TournamentSemiOver) : TurnirUiState()
    data class FinalReady(val msg: ServerMessage.TournamentFinalStarted) : TurnirUiState()
    data class FinalOngoing(val msg: ServerMessage.TournamentUpdate) : TurnirUiState()
    data class TournamentComplete(val msg: ServerMessage.TournamentOver) : TurnirUiState()
}

sealed class TurnirEvent {
    data object NavigateToBracket : TurnirEvent()
    data object Cancelled : TurnirEvent()
}
