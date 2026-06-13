package rs.tim13.slagalica.spojnice.ui

import rs.tim13.slagalica.core.model.GameResult
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.BaseGameViewModel
import rs.tim13.slagalica.core.ui.GameEvent
import rs.tim13.slagalica.spojnice.model.SpojniceGame
import rs.tim13.slagalica.spojnice.model.SpojniceRound
import rs.tim13.slagalica.spojnice.model.SpojniceStatistics

/**
 * Vodi Spojnice: dve runde (jedna u single-player), svaka sa fazom vodećeg i fazom popravka,
 * po [TURN_SECONDS] sekundi. Stanje table i bodovanje drži [SpojniceGame]; ovde se upravlja
 * tajmerima, prelaskom u popravak i sinhronizacijom poteza.
 */
class SpojniceViewModel(
    private val rounds: List<SpojniceRound>,
    localPlayer: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : BaseGameViewModel<SpojniceUiState>(
    localPlayer,
    isSinglePlayer,
    maxRounds = if (isSinglePlayer) 1 else 2,
    initialOpponentDisconnected
) {

    private lateinit var currentGame: SpojniceGame
    private var selectedLeftIndex: Int? = null

    private var blueConnected = 0
    private var blueAttempts = 0
    private var redConnected = 0
    private var redAttempts = 0

    init {
        startRound(currentRoundIndex)
    }

    override fun startRound(index: Int) {
        val initialPlayer = if (index == 0) Player.BLUE else Player.RED
        val round = rounds[index]

        currentGame = SpojniceGame(
            leftItems = round.leftItems,
            rightItems = round.rightItems,
            solution = round.solution,
            initialPlayer = initialPlayer,
            isSinglePlayer = isSinglePlayer
        )
        selectedLeftIndex = null

        if (isOpponentDisconnected && initialPlayer != localPlayer) {
            currentGame.handleOpponentDisconnect(localPlayer)
        }

        startTimer(TURN_SECONDS)
        updateSpecificState(SpojniceGamePhase.PLAYING)
    }

    fun selectLeft(leftIndex: Int) {
        if (!canPlay()) return
        if (currentGame.isLeftConnected(leftIndex)) return
        selectedLeftIndex = leftIndex
        updateSpecificState(SpojniceGamePhase.PLAYING)
    }

    fun selectRight(rightIndex: Int) {
        if (!canPlay()) return
        val leftIndex = selectedLeftIndex
        if (leftIndex == null) {
            updateSpecificState(SpojniceGamePhase.PLAYING, "Prvo izaberite pojam levo.")
            return
        }

        val correct = currentGame.attemptConnection(leftIndex, rightIndex)
        if (!isSinglePlayer) {
            events.value = GameEvent.MovePlayed(
                "CONNECT",
                mapOf("left" to leftIndex, "right" to rightIndex)
            )
        }
        selectedLeftIndex = null
        advanceAfterAttempt(if (correct) "Tačno!" else "Netačno.")
    }

    override fun onTimerTick() {
        updateSpecificState(uiState.value?.phase ?: SpojniceGamePhase.PLAYING)
    }

    override fun onTimeUp() {
        if (uiState.value?.phase != SpojniceGamePhase.PLAYING) return
        when {
            currentGame.allSolved -> endRound("Vreme je isteklo!")
            !currentGame.isRecoveryPhase && currentGame.canStartRecovery() -> enterRecovery()
            else -> endRound("Vreme je isteklo!")
        }
    }

    private fun advanceAfterAttempt(message: String) {
        when {
            currentGame.allSolved -> endRound("Sve spojnice su rešene!")
            !currentGame.isRecoveryPhase && currentGame.isLeadPhaseExhausted -> {
                if (currentGame.canStartRecovery()) enterRecovery()
                else endRound("Runda je završena.")
            }
            currentGame.isRecoveryPhase && currentGame.isRecoveryPhaseExhausted ->
                endRound("Runda je završena.")
            else -> updateSpecificState(SpojniceGamePhase.PLAYING, message)
        }
    }

    private fun enterRecovery() {
        currentGame.beginRecovery()
        selectedLeftIndex = null
        startTimer(TURN_SECONDS)
        updateSpecificState(SpojniceGamePhase.PLAYING, "Protivnik popravlja preostale parove.")
    }

    private fun endRound(message: String) {
        stopTimer()
        calculateRoundScoresAndStats()
        updateSpecificState(SpojniceGamePhase.ROUND_OVER, message)
    }

    override fun calculateRoundScoresAndStats() {
        val scores = currentGame.calculateScore()
        totalBlueScore += scores[Player.BLUE] ?: 0
        totalRedScore += scores[Player.RED] ?: 0
        blueConnected += currentGame.connectedCountFor(Player.BLUE)
        blueAttempts += currentGame.attemptsFor(Player.BLUE)
        redConnected += currentGame.connectedCountFor(Player.RED)
        redAttempts += currentGame.attemptsFor(Player.RED)
    }

    override fun finishGame() {
        updateSpecificState(SpojniceGamePhase.GAME_OVER, "Kraj Spojnica!")
        events.value = GameEvent.GameFinished(
            GameResult(
                blueScore = totalBlueScore,
                redScore = totalRedScore,
                statistics = SpojniceStatistics(blueConnected, blueAttempts, redConnected, redAttempts)
            )
        )
    }

    override fun isRoundOver(): Boolean = uiState.value?.phase == SpojniceGamePhase.ROUND_OVER

    override fun onOpponentDisconnected() {
        currentGame.handleOpponentDisconnect(localPlayer)
        if (currentGame.isRecoveryPhase) {
            endRound("Protivnik je napustio. Runda je završena.")
        } else {
            updateSpecificState(SpojniceGamePhase.PLAYING, "Protivnik je napustio igru.")
        }
    }

    override fun onRemoteMove(action: String, payload: Map<String, Any>) {
        when (action) {
            "CONNECT" -> {
                val left = (payload["left"] as Number).toInt()
                val right = (payload["right"] as Number).toInt()
                val correct = currentGame.attemptConnection(left, right)
                advanceAfterAttempt(if (correct) "Protivnik je pogodio." else "Protivnik je promašio.")
            }
            else -> super.onRemoteMove(action, payload)
        }
    }

    private fun canPlay(): Boolean =
        uiState.value?.phase == SpojniceGamePhase.PLAYING && isMyTurn()

    private fun isMyTurn(): Boolean {
        if (isSinglePlayer) return true
        return currentGame.activePlayer == localPlayer
    }

    private fun updateSpecificState(phase: SpojniceGamePhase, message: String = "") {
        val scores = currentGame.calculateScore()
        updateState(
            SpojniceUiState(
                round = currentRoundIndex + 1,
                activePlayer = currentGame.activePlayer,
                isMyTurn = isMyTurn(),
                phase = phase,
                remainingSeconds = remainingSeconds,
                statusMessage = message,
                leftItems = currentGame.leftItems,
                rightItems = currentGame.rightItems,
                connectionsByLeft = currentGame.connectionsByLeft(),
                connectedRightIndices = currentGame.connectedRightIndices(),
                selectedLeftIndex = selectedLeftIndex,
                isRecoveryPhase = currentGame.isRecoveryPhase,
                blueScore = scores[Player.BLUE] ?: 0,
                redScore = scores[Player.RED] ?: 0
            )
        )
    }

    companion object {
        const val TURN_SECONDS = 30
    }
}
