package rs.tim13.slagalica.koznazna.ui

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.GameUiState

data class KoZnaZnaUiState(
    override val round: Int,
    override val activePlayer: Player,
    override val isMyTurn: Boolean,
    override val phase: KoZnaZnaGamePhase,
    override val remainingSeconds: Int,
    override val statusMessage: String = "",
    val questionNumber: Int,
    val questionCount: Int,
    val questionText: String,
    val options: List<String>,
    val myAnswerIndex: Int?,
    val correctIndex: Int?,   // != null samo u REVEAL / GAME_OVER
    val blueScore: Int,
    val redScore: Int
) : GameUiState
