package rs.tim13.slagalica.asocijacije.ui

import rs.tim13.slagalica.asocijacije.model.AssociationsColumn
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.GameUiState

data class AssociationsUiState(
    override val round: Int,
    override val activePlayer: Player,
    override val isMyTurn: Boolean,
    override val phase: AssociationsGamePhase,
    override val remainingSeconds: Int,
    override val statusMessage: String = "",
    val columns: List<AssociationsColumn>,
    val finalSolution: String,
    val isFinalSolved: Boolean,
    val isNextMoveRevealing: Boolean = true,
) : GameUiState
