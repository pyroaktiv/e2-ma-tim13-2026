package rs.tim13.slagalica.asocijacije.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rs.tim13.slagalica.asocijacije.model.AssociationsGame
import rs.tim13.slagalica.asocijacije.model.AssociationsStatistics
import rs.tim13.slagalica.core.model.GameResult
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.BaseGameViewModel
import rs.tim13.slagalica.core.ui.GameEvent

class AssociationsViewModel(
    private val games: List<AssociationsGame>,
    localPlayer: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : BaseGameViewModel<AssociationsUiState>(localPlayer, isSinglePlayer, maxRounds = if (isSinglePlayer) 1 else 2, initialOpponentDisconnected) {

    private var blueSolvedCount = 0
    private var blueUnsolvedCount = 0
    private var redSolvedCount = 0
    private var redUnsolvedCount = 0

    private lateinit var currentGame: AssociationsGame

    init {
        startRound(currentRoundIndex)
    }

    override fun startRound(index: Int) {
        val initialPlayer = if (index == 0) Player.BLUE else Player.RED

        currentGame = AssociationsGame(
            columns = games[index].columns,
            finalSolution = games[index].finalSolution,
            initialPlayer = initialPlayer,
            isSinglePlayer = isSinglePlayer
        )

        if (isOpponentDisconnected && initialPlayer != localPlayer) {
            currentGame.handleOpponentDisconnect(localPlayer)
        }

        startTimer(120)
        updateSpecificState(AssociationsGamePhase.PLAYING)
    }

    override fun onTimerTick() {
        updateSpecificState(uiState.value?.phase ?: AssociationsGamePhase.PLAYING)
    }

    override fun onTimeUp() {
        calculateRoundScoresAndStats()
        updateSpecificState(AssociationsGamePhase.ROUND_OVER, "Vreme je isteklo!")
    }

    override fun calculateRoundScoresAndStats() {
        val scores = currentGame.calculateScore()
        totalBlueScore += scores[Player.BLUE] ?: 0
        totalRedScore += scores[Player.RED] ?: 0

        if (currentGame.isFinalSolved) {
            if (currentGame.solvedBy == Player.BLUE) {
                blueSolvedCount++
                redUnsolvedCount++
            } else {
                redSolvedCount++
                blueUnsolvedCount++
            }
        } else {
            blueUnsolvedCount++
            redUnsolvedCount++
        }
    }

    override fun finishGame() {
        updateSpecificState(AssociationsGamePhase.GAME_OVER)
        events.value = GameEvent.GameFinished(
            GameResult(
                blueScore = totalBlueScore,
                redScore = totalRedScore,
                statistics = AssociationsStatistics(
                    blueSolvedCount, blueUnsolvedCount, redSolvedCount, redUnsolvedCount
                )
            )
        )
    }

    override fun onOpponentDisconnected() {
        currentGame.handleOpponentDisconnect(localPlayer)
        updateSpecificState(AssociationsGamePhase.PLAYING, "Protivnik je napustio igru. Završite rundu sami.")
    }

    override fun isRoundOver(): Boolean {
        return uiState.value?.phase == AssociationsGamePhase.ROUND_OVER
    }

    fun revealField(columnIndex: Int, fieldIndex: Int) {
        if (!isMyTurn() || uiState.value?.phase != AssociationsGamePhase.PLAYING) return
        if (currentGame.revealField(columnIndex, fieldIndex)) {
            events.value = GameEvent.MovePlayed(
                action = "REVEAL_FIELD",
                payload = mapOf("column" to columnIndex, "field" to fieldIndex)
            )
            updateSpecificState(AssociationsGamePhase.PLAYING)
        }
    }

    fun guessColumn(columnIndex: Int, guess: String): Boolean {
        if (!isMyTurn() || uiState.value?.phase != AssociationsGamePhase.PLAYING) return false
        val isCorrect = currentGame.guessColumn(columnIndex, guess)

        events.value = GameEvent.MovePlayed(
            action = "GUESS_COLUMN",
            payload = mapOf("column" to columnIndex, "guess" to guess)
        )

        updateSpecificState(AssociationsGamePhase.PLAYING)
        return isCorrect
    }

    fun guessFinal(guess: String): Boolean {
        if (!isMyTurn() || uiState.value?.phase != AssociationsGamePhase.PLAYING) return false

        val isCorrect = currentGame.guessFinal(guess)

        events.value = GameEvent.MovePlayed(
            action = "GUESS_FINAL",
            payload = mapOf("guess" to guess)
        )

        if (isCorrect) {
            stopTimer()
            calculateRoundScoresAndStats()
            updateSpecificState(AssociationsGamePhase.ROUND_OVER, "Tačno rešenje!")
        } else {
            updateSpecificState(AssociationsGamePhase.PLAYING)
        }
        return isCorrect
    }

    private fun isMyTurn(): Boolean {
        if (isSinglePlayer) return true
        return currentGame.activePlayer == localPlayer
    }

    override fun onRemoteMove(action: String, payload: Map<String, Any>) {
        when (action) {
            "REVEAL_FIELD" -> {
                val col = (payload["column"] as Number).toInt()
                val field = (payload["field"] as Number).toInt()

                currentGame.revealField(col, field)

                updateSpecificState(AssociationsGamePhase.PLAYING)
            }
            "GUESS_COLUMN" -> {
                val col = (payload["column"] as Number).toInt()
                val guess = payload["guess"] as String

                currentGame.guessColumn(col, guess)

                updateSpecificState(AssociationsGamePhase.PLAYING)
            }
            "GUESS_FINAL" -> {
                val guess = payload["guess"] as String

                val isCorrect = currentGame.guessFinal(guess)

                if (isCorrect) {
                    stopTimer()
                    calculateRoundScoresAndStats()
                    updateSpecificState(AssociationsGamePhase.ROUND_OVER, "Protivnik je pogodio konačno rešenje!")
                } else {
                    updateSpecificState(AssociationsGamePhase.PLAYING)
                }
            }
            else -> super.onRemoteMove(action, payload)
        }
    }

    private fun updateSpecificState(phase: AssociationsGamePhase, message: String = "") {
        updateState(
            AssociationsUiState(
                columns = currentGame.columns,
                finalSolution = currentGame.finalSolution,
                isFinalSolved = currentGame.isFinalSolved,
                round = currentRoundIndex + 1,
                activePlayer = currentGame.activePlayer,
                isMyTurn = isMyTurn(),
                phase = phase,
                remainingSeconds = remainingSeconds,
                statusMessage = message,
                isNextMoveRevealing = currentGame.isNextMoveRevealing
            )
        )
    }
}
