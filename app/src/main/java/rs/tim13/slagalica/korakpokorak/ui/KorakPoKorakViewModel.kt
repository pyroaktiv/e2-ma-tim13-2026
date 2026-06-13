package rs.tim13.slagalica.korakpokorak.ui

import rs.tim13.slagalica.core.model.GameResult
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.BaseGameViewModel
import rs.tim13.slagalica.core.ui.GameEvent
import rs.tim13.slagalica.korakpokorak.model.KorakPoKorakGame
import rs.tim13.slagalica.korakpokorak.model.KorakPoKorakRound
import rs.tim13.slagalica.korakpokorak.model.KorakPoKorakStatistics

/**
 * Vodi „Korak po korak": dve runde (jedna u single-player), svaka traje [TURN_SECONDS] uz
 * otkrivanje novog traga na svakih [STEP_INTERVAL]. Ako vodeći ne pogodi, protivnik dobija
 * [RECOVERY_SECONDS] za jedan pokušaj. Stanje koraka i bodovanje drži [KorakPoKorakGame].
 */
class KorakPoKorakViewModel(
    private val rounds: List<KorakPoKorakRound>,
    localPlayer: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : BaseGameViewModel<KorakPoKorakUiState>(
    localPlayer,
    isSinglePlayer,
    maxRounds = if (isSinglePlayer) 1 else 2,
    initialOpponentDisconnected
) {

    private lateinit var game: KorakPoKorakGame

    private val blueSolvedAtStep = IntArray(KorakPoKorakGame.MAX_STEPS)
    private var blueFailed = 0
    private val redSolvedAtStep = IntArray(KorakPoKorakGame.MAX_STEPS)
    private var redFailed = 0

    init {
        startRound(currentRoundIndex)
    }

    override fun startRound(index: Int) {
        val leader = if (index == 0) Player.BLUE else Player.RED
        val round = rounds[index]

        game = KorakPoKorakGame(round.clues, round.solution, leader, isSinglePlayer)
        if (isOpponentDisconnected && leader != localPlayer) {
            game.handleOpponentDisconnect(localPlayer)
        }
        game.revealNextStep() // prvi korak je otvoren odmah

        startTimer(TURN_SECONDS)
        updateSpecificState(KorakPoKorakGamePhase.PLAYING)
    }

    fun submitGuess(attempt: String) {
        if (!isMyTurn() || uiState.value?.phase != KorakPoKorakGamePhase.PLAYING) return
        if (attempt.isBlank()) return
        if (!game.canGuess) {
            updateSpecificState(KorakPoKorakGamePhase.PLAYING, "Sačekajte sledeći trag.")
            return
        }

        val correct = game.guess(attempt)
        if (!isSinglePlayer) {
            events.value = GameEvent.MovePlayed("GUESS", mapOf("attempt" to attempt))
        }
        resolveGuess(correct, mine = true)
    }

    private fun resolveGuess(correct: Boolean, mine: Boolean) {
        when {
            correct -> endRound(if (mine) "Tačno!" else "Protivnik je pogodio!")
            game.isRecoveryPhase -> endRound("Niko nije pogodio.")
            else -> updateSpecificState(
                KorakPoKorakGamePhase.PLAYING,
                if (mine) "Netačno! Sačekajte sledeći trag." else "Protivnik je promašio."
            )
        }
    }

    override fun onTimerTick() {
        if (!game.isRecoveryPhase) {
            val elapsed = TURN_SECONDS - remainingSeconds
            val targetCount = (elapsed / STEP_INTERVAL + 1).coerceAtMost(game.clues.size)
            while (game.revealedCount < targetCount) game.revealNextStep()
        }
        updateSpecificState(uiState.value?.phase ?: KorakPoKorakGamePhase.PLAYING)
    }

    override fun onTimeUp() {
        if (uiState.value?.phase != KorakPoKorakGamePhase.PLAYING) return
        when {
            game.isRecoveryPhase -> endRound("Vreme je isteklo.")
            game.canStartRecovery() -> enterRecovery()
            else -> endRound("Niko nije pogodio.")
        }
    }

    private fun enterRecovery() {
        game.beginRecovery()
        startTimer(RECOVERY_SECONDS)
        updateSpecificState(KorakPoKorakGamePhase.PLAYING, "Protivnik pogađa!")
    }

    private fun endRound(message: String) {
        stopTimer()
        calculateRoundScoresAndStats()
        updateSpecificState(KorakPoKorakGamePhase.ROUND_OVER, message)
    }

    override fun calculateRoundScoresAndStats() {
        val scores = game.calculateScore()
        totalBlueScore += scores[Player.BLUE] ?: 0
        totalRedScore += scores[Player.RED] ?: 0

        // Statistika se vodi za vodećeg igrača runde (njegov učinak po koracima).
        if (game.isSolved && !game.solvedInRecovery) {
            val stepIndex = game.solvedAtStep - 1
            when (game.leadPlayer) {
                Player.BLUE -> blueSolvedAtStep[stepIndex]++
                Player.RED -> redSolvedAtStep[stepIndex]++
            }
        } else {
            when (game.leadPlayer) {
                Player.BLUE -> blueFailed++
                Player.RED -> redFailed++
            }
        }
    }

    override fun finishGame() {
        updateSpecificState(KorakPoKorakGamePhase.GAME_OVER, "Kraj igre!")
        events.value = GameEvent.GameFinished(
            GameResult(
                blueScore = totalBlueScore,
                redScore = totalRedScore,
                statistics = KorakPoKorakStatistics(
                    blueSolvedAtStep.toList(), blueFailed, redSolvedAtStep.toList(), redFailed
                )
            )
        )
    }

    override fun isRoundOver(): Boolean = uiState.value?.phase == KorakPoKorakGamePhase.ROUND_OVER

    override fun onOpponentDisconnected() {
        game.handleOpponentDisconnect(localPlayer)
        if (game.isRecoveryPhase) {
            endRound("Protivnik je napustio. Kraj runde.")
        } else {
            updateSpecificState(KorakPoKorakGamePhase.PLAYING, "Protivnik je napustio igru.")
        }
    }

    override fun onRemoteMove(action: String, payload: Map<String, Any>) {
        when (action) {
            "GUESS" -> {
                val attempt = payload["attempt"] as String
                val correct = game.guess(attempt)
                resolveGuess(correct, mine = false)
            }
            else -> super.onRemoteMove(action, payload)
        }
    }

    private fun isMyTurn(): Boolean {
        if (isSinglePlayer) return true
        return game.activePlayer == localPlayer
    }

    private fun updateSpecificState(phase: KorakPoKorakGamePhase, message: String = "") {
        val roundOver = phase != KorakPoKorakGamePhase.PLAYING
        updateState(
            KorakPoKorakUiState(
                round = currentRoundIndex + 1,
                activePlayer = game.activePlayer,
                isMyTurn = isMyTurn(),
                phase = phase,
                remainingSeconds = remainingSeconds,
                statusMessage = message,
                revealedClues = game.revealedClues,
                totalSteps = game.clues.size,
                canGuess = isMyTurn() && game.canGuess && phase == KorakPoKorakGamePhase.PLAYING,
                isRecoveryPhase = game.isRecoveryPhase,
                solution = if (roundOver) game.solution else null,
                blueScore = totalBlueScore,
                redScore = totalRedScore
            )
        )
    }

    companion object {
        const val TURN_SECONDS = 70
        const val STEP_INTERVAL = 10
        const val RECOVERY_SECONDS = 10
    }
}
