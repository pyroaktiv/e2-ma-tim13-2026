package rs.tim13.slagalica.skocko.ui

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.GameUiState
import rs.tim13.slagalica.skocko.model.SkockoGuess
import rs.tim13.slagalica.skocko.model.SkockoSymbol

data class SkockoUiState(
    override val round: Int,
    override val activePlayer: Player,
    override val isMyTurn: Boolean,
    override val phase: SkockoGamePhase,
    override val remainingSeconds: Int,
    override val statusMessage: String = "",
    val mainPlayer: Player,
    val mainGuesses: List<SkockoGuess>,
    val bonusGuess: SkockoGuess?,
    val secret: List<SkockoSymbol>?,
    val currentInput: List<SkockoSymbol>,
    val isInputEnabled: Boolean
) : GameUiState
