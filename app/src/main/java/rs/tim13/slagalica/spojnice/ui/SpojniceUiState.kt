package rs.tim13.slagalica.spojnice.ui

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.GameUiState

data class SpojniceUiState(
    override val round: Int,
    override val activePlayer: Player,
    override val isMyTurn: Boolean,
    override val phase: SpojniceGamePhase,
    override val remainingSeconds: Int,
    override val statusMessage: String = "",
    val leftItems: List<String>,
    val rightItems: List<String>,
    val connectionsByLeft: List<Player?>,   // ko je povezao svaki levi pojam (null = nije)
    val connectedRightIndices: Set<Int>,    // zauzeti desni pojmovi
    val selectedLeftIndex: Int?,
    val isRecoveryPhase: Boolean,
    val blueScore: Int,
    val redScore: Int
) : GameUiState
