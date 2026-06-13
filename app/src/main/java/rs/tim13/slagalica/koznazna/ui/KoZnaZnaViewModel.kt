package rs.tim13.slagalica.koznazna.ui

import rs.tim13.slagalica.core.model.GameResult
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.BaseGameViewModel
import rs.tim13.slagalica.core.ui.GameEvent
import rs.tim13.slagalica.koznazna.model.KoZnaZnaGame
import rs.tim13.slagalica.koznazna.model.KoZnaZnaQuestion
import rs.tim13.slagalica.koznazna.model.KoZnaZnaStatistics

/**
 * Vodi „Ko zna zna": jedna runda, pet pitanja po [QUESTION_SECONDS] sekundi, a između
 * pitanja kratak [REVEAL_SECONDS] prikaz tačnog odgovora. Bodovi i statistika se računaju
 * u [KoZnaZnaGame]; ovde se samo upravlja tokom i sinhronizacijom poteza.
 */
class KoZnaZnaViewModel(
    private val questions: List<KoZnaZnaQuestion>,
    localPlayer: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : BaseGameViewModel<KoZnaZnaUiState>(localPlayer, isSinglePlayer, maxRounds = 1, initialOpponentDisconnected) {

    private lateinit var game: KoZnaZnaGame

    private var blueCorrect = 0
    private var blueWrong = 0
    private var redCorrect = 0
    private var redWrong = 0

    init {
        startRound(currentRoundIndex)
    }

    override fun startRound(index: Int) {
        game = KoZnaZnaGame(questions, isSinglePlayer, isOpponentDisconnected)
        startQuestion()
    }

    private fun startQuestion() {
        startTimer(QUESTION_SECONDS)
        updateSpecificState(KoZnaZnaGamePhase.PLAYING)
    }

    fun submitAnswer(optionIndex: Int) {
        if (uiState.value?.phase != KoZnaZnaGamePhase.PLAYING) return
        if (!game.submitAnswer(localPlayer, optionIndex)) return

        if (!isSinglePlayer) {
            events.value = GameEvent.MovePlayed("ANSWER", mapOf("index" to optionIndex))
        }

        // Bez čekanja: u single-player, ako je protivnik otišao, ili kad su oba odgovorila.
        if (isSinglePlayer || isOpponentDisconnected || game.bothAnswered) {
            revealCurrentQuestion()
        } else {
            updateSpecificState(KoZnaZnaGamePhase.PLAYING)
        }
    }

    private fun revealCurrentQuestion() {
        stopTimer()
        game.resolveCurrentQuestion()
        startTimer(REVEAL_SECONDS)
        updateSpecificState(KoZnaZnaGamePhase.REVEAL)
    }

    override fun onTimerTick() {
        updateSpecificState(uiState.value?.phase ?: KoZnaZnaGamePhase.PLAYING)
    }

    override fun onTimeUp() {
        when (uiState.value?.phase) {
            KoZnaZnaGamePhase.PLAYING -> revealCurrentQuestion()
            KoZnaZnaGamePhase.REVEAL -> advance()
            else -> {}
        }
    }

    private fun advance() {
        if (game.advance()) {
            startQuestion()
        } else {
            calculateRoundScoresAndStats()
            finishGame()
        }
    }

    override fun calculateRoundScoresAndStats() {
        val scores = game.calculateScore()
        totalBlueScore += scores[Player.BLUE] ?: 0
        totalRedScore += scores[Player.RED] ?: 0
        blueCorrect = game.blueCorrect
        blueWrong = game.blueWrong
        redCorrect = game.redCorrect
        redWrong = game.redWrong
    }

    override fun finishGame() {
        updateSpecificState(KoZnaZnaGamePhase.GAME_OVER)
        events.value = GameEvent.GameFinished(
            GameResult(
                blueScore = totalBlueScore,
                redScore = totalRedScore,
                statistics = KoZnaZnaStatistics(blueCorrect, blueWrong, redCorrect, redWrong)
            )
        )
    }

    override fun isRoundOver(): Boolean = uiState.value?.phase == KoZnaZnaGamePhase.GAME_OVER

    override fun onOpponentDisconnected() {
        // Ako čekamo protivnika a mi smo već odgovorili, ne čekamo dalje.
        if (uiState.value?.phase == KoZnaZnaGamePhase.PLAYING && game.hasAnswered(localPlayer)) {
            revealCurrentQuestion()
        } else {
            updateSpecificState(
                uiState.value?.phase ?: KoZnaZnaGamePhase.PLAYING,
                "Protivnik je napustio igru."
            )
        }
    }

    override fun onRemoteMove(action: String, payload: Map<String, Any>) {
        when (action) {
            "ANSWER" -> {
                val index = (payload["index"] as Number).toInt()
                val opponent = Player.entries.first { it != localPlayer }
                game.submitAnswer(opponent, index)
                if (game.bothAnswered) revealCurrentQuestion()
                else updateSpecificState(KoZnaZnaGamePhase.PLAYING)
            }
            else -> super.onRemoteMove(action, payload)
        }
    }

    private fun updateSpecificState(phase: KoZnaZnaGamePhase, message: String = "") {
        val revealing = phase == KoZnaZnaGamePhase.REVEAL || phase == KoZnaZnaGamePhase.GAME_OVER
        val question = game.currentQuestion
        val scores = game.calculateScore()
        updateState(
            KoZnaZnaUiState(
                round = currentRoundIndex + 1,
                activePlayer = localPlayer,
                isMyTurn = true,
                phase = phase,
                remainingSeconds = remainingSeconds,
                statusMessage = message,
                questionNumber = game.currentIndex + 1,
                questionCount = game.questionCount,
                questionText = question.text,
                options = question.options,
                myAnswerIndex = game.answerOf(localPlayer)?.optionIndex,
                correctIndex = if (revealing) game.correctIndex else null,
                blueScore = scores[Player.BLUE] ?: 0,
                redScore = scores[Player.RED] ?: 0
            )
        )
    }

    companion object {
        const val QUESTION_SECONDS = 5
        const val REVEAL_SECONDS = 2
    }
}
