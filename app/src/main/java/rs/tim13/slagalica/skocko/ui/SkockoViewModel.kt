package rs.tim13.slagalica.skocko.ui

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.skocko.data.MockSkockoGameRepository
import rs.tim13.slagalica.skocko.data.SkockoGameRepository
import rs.tim13.slagalica.skocko.model.SkockoGame
import rs.tim13.slagalica.skocko.model.SkockoSymbol

class SkockoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SkockoGameRepository = MockSkockoGameRepository()

    private val _uiState = MutableLiveData<SkockoUiState>()
    val uiState: LiveData<SkockoUiState> = _uiState

    private val secrets = repository.getSecrets()
    private var game = SkockoGame(secrets[0], Player.BLUE)
    private var round = 1
    private var score = mutableMapOf(Player.BLUE to 0, Player.RED to 0)
    private val currentInput = mutableListOf<SkockoSymbol>()
    private var phase = SkockoGamePhase.MAIN_TURN
    private var remainingSeconds = MAIN_SECONDS
    private var mainTimer: CountDownTimer? = null
    private var bonusTimer: CountDownTimer? = null
    private var roundScoreAwarded = false

    companion object {
        const val MAIN_SECONDS = 30
        const val BONUS_SECONDS = 10
    }

    init {
        emit()
        startMainTimer()
    }

    fun addSymbol(symbol: SkockoSymbol) {
        if (phase != SkockoGamePhase.MAIN_TURN && phase != SkockoGamePhase.BONUS_TURN) return
        if (currentInput.size >= 4) return
        currentInput.add(symbol)
        emit()
    }

    fun eraseSymbol() {
        if (phase != SkockoGamePhase.MAIN_TURN && phase != SkockoGamePhase.BONUS_TURN) return
        if (currentInput.isNotEmpty()) currentInput.removeAt(currentInput.lastIndex)
        emit()
    }

    fun submitGuess() {
        if (currentInput.size != 4) return
        when (phase) {
            SkockoGamePhase.MAIN_TURN -> {
                game.submitMainGuess(currentInput.toList()) ?: return
                currentInput.clear()
                when {
                    game.isSolvedByMain -> {
                        mainTimer?.cancel()
                        awardRoundScore()
                        phase = if (round < 2) SkockoGamePhase.ROUND_OVER else SkockoGamePhase.GAME_OVER
                    }
                    game.isMainPhaseExhausted -> {
                        mainTimer?.cancel()
                        phase = SkockoGamePhase.BONUS_TURN
                        remainingSeconds = BONUS_SECONDS
                        emit()
                        startBonusTimer()
                        return
                    }
                }
                emit()
            }
            SkockoGamePhase.BONUS_TURN -> {
                game.submitBonusGuess(currentInput.toList()) ?: return
                currentInput.clear()
                bonusTimer?.cancel()
                awardRoundScore()
                phase = if (round < 2) SkockoGamePhase.ROUND_OVER else SkockoGamePhase.GAME_OVER
                emit()
            }
            else -> return
        }
    }

    fun advanceToNextRound() {
        if (phase != SkockoGamePhase.ROUND_OVER) return
        round = 2
        game = SkockoGame(secrets[1], Player.RED)
        currentInput.clear()
        phase = SkockoGamePhase.MAIN_TURN
        remainingSeconds = MAIN_SECONDS
        roundScoreAwarded = false
        emit()
        startMainTimer()
    }

    private fun startMainTimer() {
        mainTimer?.cancel()
        mainTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt() + 1
                emit()
            }
            override fun onFinish() {
                remainingSeconds = 0
                phase = SkockoGamePhase.BONUS_TURN
                remainingSeconds = BONUS_SECONDS
                emit()
                startBonusTimer()
            }
        }.start()
    }

    private fun startBonusTimer() {
        bonusTimer?.cancel()
        bonusTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt() + 1
                emit()
            }
            override fun onFinish() {
                remainingSeconds = 0
                awardRoundScore()
                phase = if (round < 2) SkockoGamePhase.ROUND_OVER else SkockoGamePhase.GAME_OVER
                emit()
            }
        }.start()
    }

    private fun awardRoundScore() {
        if (roundScoreAwarded) return
        roundScoreAwarded = true
        game.calculateScore().forEach { (player, pts) ->
            score.compute(player) { _, v -> (v ?: 0) + pts }
        }
    }

    override fun onCleared() {
        mainTimer?.cancel()
        bonusTimer?.cancel()
    }

    private fun emit() {
        val revealSecret = phase == SkockoGamePhase.ROUND_OVER || phase == SkockoGamePhase.GAME_OVER
        val isActive = phase == SkockoGamePhase.MAIN_TURN || phase == SkockoGamePhase.BONUS_TURN
        _uiState.value = SkockoUiState(
            round = round,
            blueScore = score[Player.BLUE] ?: 0,
            redScore = score[Player.RED] ?: 0,
            mainPlayer = game.initialPlayer,
            phase = phase,
            remainingSeconds = remainingSeconds,
            mainGuesses = game.mainGuesses,
            bonusGuess = game.bonusGuess,
            secret = if (revealSecret) game.secret else null,
            currentInput = currentInput.toList(),
            isInputEnabled = isActive && remainingSeconds > 0,
            statusMessage = buildStatusMessage()
        )
    }

    private fun buildStatusMessage(): String {
        val ctx = getApplication<Application>()
        val opponent = Player.entries.first { it != game.initialPlayer }
        return when (phase) {
            SkockoGamePhase.MAIN_TURN ->
                ctx.getString(R.string.skocko_status_main_turn, game.initialPlayer.color)
            SkockoGamePhase.BONUS_TURN ->
                ctx.getString(R.string.skocko_status_bonus_turn, opponent.color)
            SkockoGamePhase.ROUND_OVER -> when {
                game.isSolvedByMain ->
                    ctx.getString(R.string.skocko_status_solved_main, game.initialPlayer.color)
                game.isSolvedByBonus ->
                    ctx.getString(R.string.skocko_status_solved_bonus, opponent.color)
                else ->
                    ctx.getString(R.string.skocko_status_no_one)
            }
            SkockoGamePhase.GAME_OVER -> {
                val blue = score[Player.BLUE] ?: 0
                val red = score[Player.RED] ?: 0
                val winner = when {
                    blue > red -> Player.BLUE
                    red > blue -> Player.RED
                    else -> null
                }
                if (winner != null)
                    ctx.getString(R.string.skocko_status_winner, winner.color)
                else
                    ctx.getString(R.string.skocko_status_draw)
            }
        }
    }
}
