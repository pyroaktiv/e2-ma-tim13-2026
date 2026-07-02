package rs.tim13.slagalica.skocko.ui

import rs.tim13.slagalica.core.model.GameResult
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.BaseGameViewModel
import rs.tim13.slagalica.core.ui.GameEvent
import rs.tim13.slagalica.skocko.model.SkockoGame
import rs.tim13.slagalica.skocko.model.SkockoStatistics
import rs.tim13.slagalica.skocko.model.SkockoSymbol

class SkockoViewModel(
    private val secrets: List<List<SkockoSymbol>>,
    localPlayer: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : BaseGameViewModel<SkockoUiState>(
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

        if (isOpponentDisconnected) {
            currentGame.onOpponentLeft(localPlayer)
        }

        startTimer(30)
        updateSpecificState()
    }

    override fun onTimerTick() {
        updateSpecificState()
    }

    override fun onTimeUp() {
        if (currentPhase == SkockoGamePhase.MAIN_TURN) {
            if (isSinglePlayer) {
                finishRoundLogic()
            } else {
                currentGame.forfeitMainTurn()
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
        scheduleRoundAdvance()
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
                if (isSinglePlayer) {
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

        val mainPlayer = currentGame.mainPlayer
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
        events.value = GameEvent.GameFinished(
            GameResult(
                blueScore = totalBlueScore,
                redScore = totalRedScore,
                statistics = SkockoStatistics(
                    blueCorrectAtAttempt.toList(), blueFailed,
                    redCorrectAtAttempt.toList(), redFailed
                )
            )
        )
    }

    override fun onOpponentDisconnected() {
        currentGame.onOpponentLeft(localPlayer)
        updateSpecificState("Protivnik je napustio igru. Imate šansu da sami osvojite bodove.")
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
                    if (isSinglePlayer) {
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
                mainPlayer = currentGame.mainPlayer,
                mainGuesses = currentGame.mainGuesses,
                bonusGuess = currentGame.bonusGuess,
                secret = if (revealSecret) currentGame.secret else null,
                currentInput = currentInput.toList(),
                isInputEnabled = isInputActive() && isMyTurn(),
                blueScore = totalBlueScore,
                redScore = totalRedScore
            )
        )
    }
}
