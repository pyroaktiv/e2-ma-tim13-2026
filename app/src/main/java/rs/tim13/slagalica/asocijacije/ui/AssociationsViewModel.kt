package rs.tim13.slagalica.asocijacije.ui

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import rs.tim13.slagalica.asocijacije.data.MockAssociationsGameRepository
import rs.tim13.slagalica.asocijacije.model.AssociationsGame
import rs.tim13.slagalica.asocijacije.model.AssociationsGameRepository
import rs.tim13.slagalica.core.Player

class AssociationsViewModel(
    private val repository: AssociationsGameRepository = MockAssociationsGameRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<AssociationsUiState>()
    val uiState: LiveData<AssociationsUiState> = _uiState

    private val games: List<AssociationsGame> = repository.getGames()
    private var game = games[0]
    private var round = 1
    private var score = mutableMapOf(Player.BLUE to 0, Player.RED to 0)
    private var roundScoreAwarded = false

    private var remainingSeconds = ROUND_SECONDS
    private var timer: CountDownTimer? = null

    companion object {
        const val ROUND_SECONDS = 120
    }

    init {
        emit(GamePhase.PLAYING)
        startTimer()
    }

    fun revealField(columnIndex: Int, fieldIndex: Int) {
        if (phase() != GamePhase.PLAYING) return
        if (game.revealField(columnIndex, fieldIndex)) emit(GamePhase.PLAYING)
    }

    fun guessColumn(columnIndex: Int, guess: String): Boolean {
        if (phase() != GamePhase.PLAYING) return false
        val correct = game.guessColumn(columnIndex, guess)
        emit(GamePhase.PLAYING)
        return correct
    }

    fun guessFinal(guess: String): Boolean {
        if (phase() != GamePhase.PLAYING) return false
        val correct = game.guessFinal(guess)
        if (correct) {
            timer?.cancel()
            awardRoundScore()
            emit(if (round < 2) GamePhase.ROUND_OVER else GamePhase.GAME_OVER)
        } else {
            emit(GamePhase.PLAYING)
        }
        return correct
    }

    fun advanceToNextRound() {
        if (phase() != GamePhase.ROUND_OVER) return
        round = 2
        game = AssociationsGame(games[1].columns, games[1].finalSolution, Player.RED)
        remainingSeconds = ROUND_SECONDS
        roundScoreAwarded = false
        emit(GamePhase.PLAYING)
        startTimer()
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(remainingSeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt() + 1
                emit(GamePhase.PLAYING)
            }
            override fun onFinish() {
                remainingSeconds = 0
                awardRoundScore()
                emit(if (round < 2) GamePhase.ROUND_OVER else GamePhase.GAME_OVER)
            }
        }.start()
    }

    private fun awardRoundScore() {
        if (roundScoreAwarded) return
        roundScoreAwarded = true
        game.calculateScore().entries.forEach {
            score.compute(it.key) { _, v -> (v ?: 0) + it.value }
        }
    }

    override fun onCleared() {
        timer?.cancel()
    }

    private fun phase(): GamePhase = _uiState.value?.phase ?: GamePhase.PLAYING

    private fun emit(phase: GamePhase) {
        _uiState.value = AssociationsUiState(
            columns = game.columns,
            finalSolution = game.finalSolution,
            isFinalSolved = game.isFinalSolved,
            round = round,
            blueScore = score[Player.BLUE] ?: 0,
            redScore = score[Player.RED] ?: 0,
            activePlayer = game.activePlayer,
            phase = phase,
            remainingSeconds = remainingSeconds,
            isNextMoveRevealing = game.isNextMoveRevealing,
        )
    }
}
