package rs.tim13.slagalica.asocijacije.ui

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
    private var activePlayer = Player.BLUE

    init {
        emit(GamePhase.PLAYING, true)
    }

    fun revealField(columnIndex: Int, fieldIndex: Int) {
        if (phase() != GamePhase.PLAYING) return
        if (!shouldReveal()) return

        game.revealField(columnIndex, fieldIndex)
        emit(GamePhase.PLAYING, false)
    }

    fun guessColumn(columnIndex: Int, guess: String): Boolean {
        if (phase() != GamePhase.PLAYING) return false
        if (shouldReveal()) return false

        val correct = game.guessColumn(columnIndex, guess, activePlayer)
        if (correct)  {
            emit(GamePhase.PLAYING, false)
        } else {
            switchPlayer()
            emit(GamePhase.PLAYING, true)
        }

        return correct
    }

    fun guessFinal(guess: String): Boolean {
        if (phase() != GamePhase.PLAYING) return false
        if (shouldReveal()) return false

        val correct = game.guessFinal(guess, activePlayer)
        if (!correct) switchPlayer()
        return correct.also { checkCompletion() }
    }

    fun advanceToNextRound() {
        if(phase() == GamePhase.GAME_OVER) return

        awardRoundScore()
        if (round == 1) {
            round = 2
            activePlayer = Player.RED
            game = games[1]
            emit(GamePhase.PLAYING, true)
        } else {
            emit(GamePhase.GAME_OVER, false)
        }
    }

    private fun checkCompletion() {
        if (game.isFinalSolved) {
            awardRoundScore()
            emit(if (round < 2) GamePhase.ROUND_OVER else GamePhase.GAME_OVER, false)
        } else {
            emit(GamePhase.PLAYING, true)
        }
    }

    private fun awardRoundScore() {
        game.calculateScore().entries.forEach {
            score.compute(it.key) {_ ,v -> (v?:0) + it.value}
        }
    }

    private fun switchPlayer() {
        activePlayer = Player.entries.filter { it != activePlayer }[0]
    }

    private fun phase(): GamePhase = _uiState.value?.phase ?: GamePhase.PLAYING
    private fun shouldReveal(): Boolean = _uiState.value?.isNextMoveRevealing ?: true

    private fun emit(phase: GamePhase, isNextMoveRevealing: Boolean) {
        _uiState.value = AssociationsUiState(
            columns = game.columns,
            finalSolution = game.finalSolution,
            isFinalSolved = game.isFinalSolved,
            round = round,
            blueScore = score[Player.BLUE] ?: 0,
            redScore = score[Player.RED] ?: 0,
            activePlayer = activePlayer,
            phase = phase,
            isNextMoveRevealing = isNextMoveRevealing,
        )
    }
}
