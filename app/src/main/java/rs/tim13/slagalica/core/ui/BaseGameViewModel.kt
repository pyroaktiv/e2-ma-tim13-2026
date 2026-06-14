package rs.tim13.slagalica.core.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rs.tim13.slagalica.core.model.Player

abstract class BaseGameViewModel<S : GameUiState>(
    protected val localPlayer: Player,
    protected val isSinglePlayer: Boolean,
    protected val maxRounds: Int,
    initialOpponentDisconnected: Boolean = false
) : ViewModel(), RemoteGame {
    private val _uiState = MutableLiveData<S>()
    val uiState: LiveData<S> = _uiState

    val events = MutableLiveData<GameEvent>()

    protected var currentRoundIndex = 0
    protected var totalBlueScore = 0
    protected var totalRedScore = 0

    protected var isOpponentDisconnected = initialOpponentDisconnected

    private var timerJob: Job? = null
    private var roundAdvanceJob: Job? = null
    protected var remainingSeconds = 0

    protected fun updateState(newState: S) {
        _uiState.value = newState
    }

    protected fun startTimer(durationSeconds: Int) {
        timerJob?.cancel()
        remainingSeconds = durationSeconds
        timerJob = viewModelScope.launch {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                onTimerTick()
            }
            onTimeUp()
        }
    }

    protected fun stopTimer() {
        timerJob?.cancel()
    }

    abstract fun onTimerTick()

    abstract fun onTimeUp()

    abstract fun startRound(index: Int)

    abstract fun calculateRoundScoresAndStats()

    override fun onRemoteMove(action: String, payload: Map<String, Any>) {
        // Podrazumevano ništa; igre koje sinhronizuju poteze override-uju ovu metodu.
    }

    abstract fun finishGame()

    /**
     * Posle kratke pauze ([ROUND_GAP_MS]) automatski prelazi na sledeću rundu ili završava igru.
     * Zamenjuje nekadašnje dugme „Sledeća runda" — oba klijenta nezavisno napreduju po isteku pauze.
     */
    protected fun scheduleRoundAdvance() {
        roundAdvanceJob?.cancel()
        roundAdvanceJob = viewModelScope.launch {
            delay(ROUND_GAP_MS)
            executeRoundTransition()
        }
    }

    private fun executeRoundTransition() {
        currentRoundIndex++
        if (currentRoundIndex < maxRounds) {
            startRound(currentRoundIndex)
        } else {
            finishGame()
        }
    }

    override fun handleOpponentDisconnected() {
        if (isSinglePlayer) return
        isOpponentDisconnected = true
        onOpponentDisconnected()
    }

    protected abstract fun onOpponentDisconnected()

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        roundAdvanceJob?.cancel()
    }

    companion object {
        const val ROUND_GAP_MS = 5000L
    }
}