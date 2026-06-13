package rs.tim13.slagalica.skocko.ui

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.skocko.model.SkockoGuess
import rs.tim13.slagalica.skocko.model.SkockoSymbol

enum class SkockoGamePhase {
    MAIN_TURN,
    BONUS_TURN,
    ROUND_OVER,
    GAME_OVER
}

data class SkockoUiState(
    val round: Int,
    val blueScore: Int,
    val redScore: Int,
    val mainPlayer: Player,
    val phase: SkockoGamePhase,
    val remainingSeconds: Int,
    val mainGuesses: List<SkockoGuess>,
    val bonusGuess: SkockoGuess?,
    val secret: List<SkockoSymbol>?,
    val currentInput: List<SkockoSymbol>,
    val isInputEnabled: Boolean,
    val statusMessage: String = ""
)
