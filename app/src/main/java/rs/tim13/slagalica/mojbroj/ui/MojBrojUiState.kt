package rs.tim13.slagalica.mojbroj.ui

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.GameUiState

data class MojBrojUiState(
    override val round: Int,
    override val activePlayer: Player,
    override val isMyTurn: Boolean,
    override val phase: MojBrojGamePhase,
    override val remainingSeconds: Int,
    override val statusMessage: String = "",
    val target: Int?,                 // != null kada je otkriven
    val numbers: List<Int>?,          // != null kada su otkriveni
    val usedNumberIndices: Set<Int>,
    val expressionDisplay: String,
    val isExpressionComplete: Boolean,
    val canStop: Boolean,             // u fazama biranja
    val isSolving: Boolean,
    val blueScore: Int,
    val redScore: Int
) : GameUiState
