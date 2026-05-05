package rs.tim13.slagalica.asocijacije.ui

import rs.tim13.slagalica.asocijacije.model.AssociationsColumn
import rs.tim13.slagalica.core.Player

enum class GamePhase { PLAYING, ROUND_OVER, GAME_OVER }

data class AssociationsUiState(
    val columns: List<AssociationsColumn>,
    val finalSolution: String,
    val isFinalSolved: Boolean,
    val round: Int,
    val blueScore: Int,
    val redScore: Int,
    val activePlayer: Player,
    val phase: GamePhase,
    val statusMessage: String = "",
    val isNextMoveRevealing: Boolean = true,
)
