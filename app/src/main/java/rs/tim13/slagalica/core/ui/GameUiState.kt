package rs.tim13.slagalica.core.ui

import rs.tim13.slagalica.core.model.Player

interface GameUiState {
    val round: Int
    val activePlayer: Player
    val phase: GamePhase
    val remainingSeconds: Int
    val statusMessage: String
    val isMyTurn: Boolean
}