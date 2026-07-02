package rs.tim13.slagalica.korakpokorak.ui

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.GameUiState

data class KorakPoKorakUiState(
    override val round: Int,
    override val activePlayer: Player,
    override val isMyTurn: Boolean,
    override val phase: KorakPoKorakGamePhase,
    override val remainingSeconds: Int,
    override val statusMessage: String = "",
    val revealedClues: List<String>,   // otkriveni tragovi (od najtežeg)
    val totalSteps: Int,
    val canGuess: Boolean,
    val isRecoveryPhase: Boolean,
    val solution: String?,             // != null kada je runda gotova
    val blueScore: Int,
    val redScore: Int
) : GameUiState
