package rs.tim13.slagalica.skocko.ui

import android.app.Application
import android.os.CountDownTimer
import android.widget.BaseAdapter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.model.BaseGame
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.BaseGameViewModel
import rs.tim13.slagalica.core.ui.GameEvent
import rs.tim13.slagalica.skocko.data.MockSkockoGameRepository
import rs.tim13.slagalica.skocko.data.SkockoGameRepository
import rs.tim13.slagalica.skocko.model.SkockoGame
import rs.tim13.slagalica.skocko.model.SkockoSymbol

sealed class SkockoEvent : GameEvent() {
    data class SkockoGameFinished(
        override val totalBlueScore: Int,
        override val totalRedScore: Int,
        val blueCorrectAtAttempt: List<Int>,
        val blueFailed: Int,
        val redCorrectAtAttempt: List<Int>,
        val redFailed: Int
    ) : GameEvent.GameFinished(totalBlueScore, totalRedScore)
}

class SkockoViewModel(
    private val secrets: List<List<SkockoSymbol>>,
    localPlayer: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : BaseGameViewModel<SkockoUiState, GameEvent>(
    localPlayer,
    isSinglePlayer,
    maxRounds = if (isSinglePlayer) 1 else 2,
    initialOpponentDisconnected
) {
    private lateinit var currentGame: SkockoGame
    private val currentInput = mutableListOf<SkockoSymbol>()
    private var currentPhase = SkockoGamePhase.MAIN_TURN

    private val blueCorrectAtAttempt = IntArray(6) { 0 }
    private var blueFailed = 0
    private val redCorrectAtAttempt = IntArray(6) { 0 }
    private var redFailed = 0

    init {
        startRound(currentRoundIndex)
    }

    override fun startRound(index: Int) {
        val initialPlayer = if (index == 0) Player.BLUE else Player.RED

        currentGame = SkockoGame(secrets[index], initialPlayer, isSinglePlayer)
        currentInput.clear()
        currentPhase = SkockoGamePhase.MAIN_TURN

        if (isOpponentDisconnected && initialPlayer != localPlayer) {
            currentGame.handleOpponentDisconnect(localPlayer)
        }

        startTimer(30)
        updateSpecificState()
    }

    override fun onTimerTick() {
        updateSpecificState()
    }

    override fun onTimeUp() {
        if (currentPhase == SkockoGamePhase.MAIN_TURN) {
            if (isSinglePlayer || currentGame.isOpponentDisconnected) {
                finishRoundLogic()
            } else {
                currentPhase = SkockoGamePhase.BONUS_TURN
                currentInput.clear()
                startTimer(10)
                updateSpecificState("Vreme je isteklo! Protivnik ima šansu.")
            }
        } else if (currentPhase == SkockoGamePhase.BONUS_TURN) {
            finishRoundLogic()
        }
    }

    private fun finishRoundLogic() {
        stopTimer()
        calculateRoundScoresAndStats()
        currentPhase = SkockoGamePhase.ROUND_OVER
        updateSpecificState("Runda je završena.")
    }

    fun addSymbol(symbol: SkockoSymbol) {
        if (!isMyTurn() || !isInputActive()) return
        if (currentInput.size >= 4) return
        currentInput.add(symbol)
        updateSpecificState()
    }

    fun eraseSymbol() {
        if (!isMyTurn() || !isInputActive()) return
        if (currentInput.isNotEmpty()) currentInput.removeAt(currentInput.lastIndex)
        updateSpecificState()
    }

    fun submitGuess() {
        if (!isMyTurn() || !isInputActive()) return
        if (currentInput.size != 4) return

        if (currentPhase == SkockoGamePhase.MAIN_TURN) {
            val guess = currentGame.submitMainGuess(currentInput.toList()) ?: return
            currentInput.clear()

            events.value = GameEvent.MovePlayed("MAIN_GUESS", mapOf("symbols" to guess.symbols.map { it.name }))

            if (currentGame.isSolvedByMain) {
                finishRoundLogic()
            } else if (currentGame.isMainPhaseExhausted) {
                // Promašeno svih 6 puta
                if (isSinglePlayer || currentGame.isOpponentDisconnected) {
                    finishRoundLogic()
                } else {
                    currentPhase = SkockoGamePhase.BONUS_TURN
                    startTimer(10)
                    updateSpecificState("Netačno! Protivnik ima šansu.")
                }
            } else {
                updateSpecificState()
            }
        } else if (currentPhase == SkockoGamePhase.BONUS_TURN) {
            val guess = currentGame.submitBonusGuess(currentInput.toList()) ?: return
            currentInput.clear()

            events.value = GameEvent.MovePlayed("BONUS_GUESS", mapOf("symbols" to guess.symbols.map { it.name }))

            finishRoundLogic()
        }
    }

    override fun calculateRoundScoresAndStats() {
        val scores = currentGame.calculateScore()
        totalBlueScore += scores[Player.BLUE] ?: 0
        totalRedScore += scores[Player.RED] ?: 0

        val mainPlayer = currentGame.initialPlayer
        if (currentGame.isSolvedByMain) {
            val attemptIndex = currentGame.mainAttemptsUsed - 1 // 0-indeksirano
            if (mainPlayer == Player.BLUE) blueCorrectAtAttempt[attemptIndex]++
            else redCorrectAtAttempt[attemptIndex]++
        } else {
            if (mainPlayer == Player.BLUE) blueFailed++
            else redFailed++
        }
    }

    override fun finishGame() {
        currentPhase = SkockoGamePhase.GAME_OVER
        updateSpecificState("Igra je završena!")
        events.value = SkockoEvent.SkockoGameFinished(
            totalBlueScore, totalRedScore,
            blueCorrectAtAttempt.toList(), blueFailed,
            redCorrectAtAttempt.toList(), redFailed
        )
    }

    override fun isRoundOver(): Boolean {
        return uiState.value?.phase == SkockoGamePhase.ROUND_OVER
    }

    override fun onOpponentDisconnected() {
        currentGame.handleOpponentDisconnect(localPlayer)

        if (currentPhase == SkockoGamePhase.BONUS_TURN && currentGame.activePlayer == localPlayer) {
            finishRoundLogic()
        } else {
            updateSpecificState("Protivnik je napustio igru.")
        }
    }

    private fun isMyTurn(): Boolean {
        if (isSinglePlayer) return true
        return currentGame.activePlayer == localPlayer
    }

    private fun isInputActive(): Boolean {
        return (currentPhase == SkockoGamePhase.MAIN_TURN || currentPhase == SkockoGamePhase.BONUS_TURN) && remainingSeconds > 0
    }

    override fun onRemoteMove(action: String, payload: Map<String, Any>) {
        when (action) {
            "MAIN_GUESS" -> {
                val rawSymbols = payload["symbols"] as List<*>
                val symbols = rawSymbols.map {
                    if (it is SkockoSymbol) it else SkockoSymbol.valueOf(it.toString())
                }

                currentGame.submitMainGuess(symbols)

                if (currentGame.isSolvedByMain) {
                    finishRoundLogic()
                } else if (currentGame.isMainPhaseExhausted) {
                    if (isSinglePlayer || currentGame.isOpponentDisconnected) {
                        finishRoundLogic()
                    } else {
                        currentPhase = SkockoGamePhase.BONUS_TURN
                        startTimer(10)
                        updateSpecificState("Protivnik nije uspeo! Vaša šansa za bonus bodove.")
                    }
                } else {
                    updateSpecificState()
                }
            }
            "BONUS_GUESS" -> {
                val rawSymbols = payload["symbols"] as List<*>
                val symbols = rawSymbols.map {
                    if (it is SkockoSymbol) it else SkockoSymbol.valueOf(it.toString())
                }

                currentGame.submitBonusGuess(symbols)

                finishRoundLogic()
            }
            else -> super.onRemoteMove(action, payload)
        }
    }

    private fun updateSpecificState(message: String = "") {
        val revealSecret = currentPhase == SkockoGamePhase.ROUND_OVER || currentPhase == SkockoGamePhase.GAME_OVER

        updateState(
            SkockoUiState(
                round = currentRoundIndex + 1,
                activePlayer = currentGame.activePlayer,
                isMyTurn = isMyTurn(),
                phase = currentPhase,
                remainingSeconds = remainingSeconds,
                statusMessage = message,
                mainPlayer = currentGame.initialPlayer,
                mainGuesses = currentGame.mainGuesses,
                bonusGuess = currentGame.bonusGuess,
                secret = if (revealSecret) currentGame.secret else null,
                currentInput = currentInput.toList(),
                isInputEnabled = isInputActive() && isMyTurn()
            )
        )
    }
}
