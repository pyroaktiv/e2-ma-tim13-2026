package rs.tim13.slagalica.mojbroj.ui

import rs.tim13.slagalica.core.model.GameResult
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.BaseGameViewModel
import rs.tim13.slagalica.core.ui.GameEvent
import rs.tim13.slagalica.mojbroj.model.MojBrojExpression
import rs.tim13.slagalica.mojbroj.model.MojBrojGame
import rs.tim13.slagalica.mojbroj.model.MojBrojRound
import rs.tim13.slagalica.mojbroj.model.MojBrojStatistics

/**
 * Vodi „Moj broj": dve runde (jedna u single-player). Svaka runda ima dve „stop" faze
 * (otkrivanje traženog broja, pa šest brojeva) sa auto-stopom posle [AUTO_STOP_SECONDS],
 * a zatim [SOLVE_SECONDS] za sastavljanje izraza. Sastavljanje izraza drži [MojBrojExpression],
 * bodovanje [MojBrojGame].
 */
class MojBrojViewModel(
    private val rounds: List<MojBrojRound>,
    localPlayer: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : BaseGameViewModel<MojBrojUiState>(
    localPlayer,
    isSinglePlayer,
    maxRounds = if (isSinglePlayer) 1 else 2,
    initialOpponentDisconnected
) {

    private lateinit var game: MojBrojGame
    private val expression = MojBrojExpression()
    private var currentPhase = MojBrojGamePhase.SELECT_TARGET

    private var blueFound = 0
    private var bluePlayed = 0
    private var redFound = 0
    private var redPlayed = 0

    init {
        startRound(currentRoundIndex)
    }

    override fun startRound(index: Int) {
        val roundLeader = if (index == 0) Player.BLUE else Player.RED
        val round = rounds[index]

        game = MojBrojGame(round.target, round.numbers, roundLeader, isSinglePlayer, isOpponentDisconnected)
        expression.clear()
        currentPhase = MojBrojGamePhase.SELECT_TARGET

        startTimer(AUTO_STOP_SECONDS)
        updateSpecificState()
    }

    /** „Stop" — klikom na dugme ili shake senzorom; otkriva traženi broj pa brojeve. */
    fun requestStop() {
        when (currentPhase) {
            MojBrojGamePhase.SELECT_TARGET -> {
                currentPhase = MojBrojGamePhase.SELECT_NUMBERS
                startTimer(AUTO_STOP_SECONDS)
                updateSpecificState()
            }
            MojBrojGamePhase.SELECT_NUMBERS -> {
                currentPhase = MojBrojGamePhase.SOLVING
                startTimer(SOLVE_SECONDS)
                updateSpecificState()
            }
            else -> Unit
        }
    }

    fun addNumber(numberIndex: Int) = withSolving {
        val value = game.numbers.getOrNull(numberIndex) ?: return@withSolving
        if (expression.addNumber(value, numberIndex)) updateSpecificState()
    }

    fun addOperator(symbol: String) = withSolving {
        if (expression.addOperator(symbol)) updateSpecificState()
    }

    fun addOpenBracket() = withSolving {
        if (expression.addOpenBracket()) updateSpecificState()
    }

    fun addCloseBracket() = withSolving {
        if (expression.addCloseBracket()) updateSpecificState()
    }

    fun backspace() = withSolving {
        if (expression.removeLast() != null) updateSpecificState()
    }

    fun submitSolution() {
        if (currentPhase != MojBrojGamePhase.SOLVING || game.hasSubmitted(localPlayer)) return
        val value = expression.evaluate()
        game.submitResult(localPlayer, value)

        if (!isSinglePlayer) {
            events.value = GameEvent.MovePlayed(
                "SUBMIT",
                mapOf("value" to (value ?: 0), "present" to (value != null))
            )
        }

        if (isSinglePlayer || isOpponentDisconnected || game.bothSubmitted) {
            resolveSolving()
        } else {
            updateSpecificState("Čekamo protivnika...")
        }
    }

    override fun onTimerTick() {
        updateSpecificState()
    }

    override fun onTimeUp() {
        when (currentPhase) {
            MojBrojGamePhase.SELECT_TARGET, MojBrojGamePhase.SELECT_NUMBERS -> requestStop()
            MojBrojGamePhase.SOLVING -> {
                if (!game.hasSubmitted(localPlayer)) game.submitResult(localPlayer, expression.evaluate())
                resolveSolving()
            }
            else -> Unit
        }
    }

    private fun resolveSolving() {
        stopTimer()
        if (!game.hasSubmitted(localPlayer)) game.submitResult(localPlayer, expression.evaluate())
        calculateRoundScoresAndStats()
        currentPhase = MojBrojGamePhase.ROUND_OVER
        updateSpecificState(resultMessage())
        scheduleRoundAdvance()
    }

    private fun resultMessage(): String = when {
        game.foundTarget(localPlayer) -> "Tačno! Pronašli ste traženi broj."
        game.resultOf(localPlayer) != null -> "Vaš rezultat: ${game.resultOf(localPlayer)}"
        else -> "Niste uneli izraz."
    }

    override fun calculateRoundScoresAndStats() {
        val scores = game.calculateScore()
        totalBlueScore += scores[Player.BLUE] ?: 0
        totalRedScore += scores[Player.RED] ?: 0

        val participants = if (isSinglePlayer) listOf(localPlayer) else listOf(Player.BLUE, Player.RED)
        participants.forEach { player ->
            val found = game.foundTarget(player)
            when (player) {
                Player.BLUE -> { bluePlayed++; if (found) blueFound++ }
                Player.RED -> { redPlayed++; if (found) redFound++ }
            }
        }
    }

    override fun finishGame() {
        currentPhase = MojBrojGamePhase.GAME_OVER
        updateSpecificState("Kraj igre!")
        events.value = GameEvent.GameFinished(
            GameResult(
                blueScore = totalBlueScore,
                redScore = totalRedScore,
                statistics = MojBrojStatistics(blueFound, bluePlayed, redFound, redPlayed)
            )
        )
    }

    override fun onOpponentDisconnected() {
        if (currentPhase == MojBrojGamePhase.SOLVING && game.hasSubmitted(localPlayer)) {
            resolveSolving()
        } else {
            updateSpecificState("Protivnik je napustio igru.")
        }
    }

    override fun onRemoteMove(action: String, payload: Map<String, Any>) {
        when (action) {
            "SUBMIT" -> {
                val present = payload["present"] as? Boolean ?: true
                val value = if (present) (payload["value"] as Number).toInt() else null
                val opponent = Player.entries.first { it != localPlayer }
                game.submitResult(opponent, value)
                if (game.hasSubmitted(localPlayer)) resolveSolving()
                else updateSpecificState("Protivnik je predao rešenje.")
            }
            else -> super.onRemoteMove(action, payload)
        }
    }

    private inline fun withSolving(block: () -> Unit) {
        if (currentPhase != MojBrojGamePhase.SOLVING || game.hasSubmitted(localPlayer)) return
        block()
    }

    private fun updateSpecificState(message: String = "") {
        val targetVisible = currentPhase != MojBrojGamePhase.SELECT_TARGET
        val numbersVisible = currentPhase == MojBrojGamePhase.SOLVING ||
                currentPhase == MojBrojGamePhase.ROUND_OVER ||
                currentPhase == MojBrojGamePhase.GAME_OVER

        updateState(
            MojBrojUiState(
                round = currentRoundIndex + 1,
                activePlayer = localPlayer,
                isMyTurn = true,
                phase = currentPhase,
                remainingSeconds = remainingSeconds,
                statusMessage = message,
                target = if (targetVisible) game.target else null,
                numbers = if (numbersVisible) game.numbers else null,
                usedNumberIndices = expression.usedNumberIndices,
                expressionDisplay = expression.display,
                isExpressionComplete = expression.isComplete,
                canStop = currentPhase == MojBrojGamePhase.SELECT_TARGET ||
                        currentPhase == MojBrojGamePhase.SELECT_NUMBERS,
                isSolving = currentPhase == MojBrojGamePhase.SOLVING,
                blueScore = totalBlueScore,
                redScore = totalRedScore
            )
        )
    }

    companion object {
        const val AUTO_STOP_SECONDS = 5
        const val SOLVE_SECONDS = 60
    }
}
