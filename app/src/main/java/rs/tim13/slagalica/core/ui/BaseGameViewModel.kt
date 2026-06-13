package rs.tim13.slagalica.core.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rs.tim13.slagalica.core.model.Player

abstract class BaseGameViewModel<S : GameUiState, E : GameEvent>(
    protected val localPlayer: Player,
    protected val isSinglePlayer: Boolean,
    protected val maxRounds: Int
) : ViewModel() {
    private val _uiState = MutableLiveData<S>()
    val uiState: LiveData<S> = _uiState

    val events = MutableLiveData<E>()

    protected var currentRoundIndex = 0
    protected var totalBlueScore = 0
    protected var totalRedScore = 0

    private var timerJob: Job? = null
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

    abstract fun finishGame()

    fun advanceToNextRound() {
        currentRoundIndex++
        if (currentRoundIndex < maxRounds) {
            startRound(currentRoundIndex)
        } else {
            finishGame()
        }
    }

    fun handleOpponentDisconnected() {
        if (isSinglePlayer) return
        onOpponentDisconnected()
    }

    protected abstract fun onOpponentDisconnected()

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}