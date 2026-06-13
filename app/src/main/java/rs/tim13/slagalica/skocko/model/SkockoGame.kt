package rs.tim13.slagalica.skocko.model

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.model.TurnBasedGame

class SkockoGame(
    val secret: List<SkockoSymbol>,
    initialPlayer: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : TurnBasedGame(initialPlayer, isSinglePlayer, initialOpponentDisconnected) {
    init {
        require(secret.size == COMBINATION_SIZE)
    }

    private val _mainGuesses: MutableList<SkockoGuess> = mutableListOf()
    val mainGuesses: List<SkockoGuess> get() = _mainGuesses.toList()

    private var _bonusGuess: SkockoGuess? = null
    val bonusGuess: SkockoGuess? get() = _bonusGuess

    var isSolvedByMain: Boolean = false
        private set

    var isSolvedByBonus: Boolean = false
        private set

    val mainAttemptsUsed: Int get() = _mainGuesses.size

    val isMainPhaseExhausted: Boolean
        get() = isSolvedByMain || mainAttemptsUsed >= MAX_MAIN_ATTEMPTS

    val hasBonusAttempt: Boolean get() = _bonusGuess != null

    companion object {
        const val MAX_MAIN_ATTEMPTS = 6
        const val COMBINATION_SIZE = 4
    }

    fun submitMainGuess(symbols: List<SkockoSymbol>): SkockoGuess? {
        require(symbols.size == COMBINATION_SIZE)
        if (isMainPhaseExhausted) return null
        val guess = SkockoGuess(symbols, evaluate(symbols))
        _mainGuesses.add(guess)
        if (guess.hints.all { it == SkockoHint.CORRECT }) {
            isSolvedByMain = true
        } else if (isMainPhaseExhausted) {
            if (!isSinglePlayer && !isOpponentDisconnected) {
                switchPlayer()
            }
        }
        return guess
    }

    fun submitBonusGuess(symbols: List<SkockoSymbol>): SkockoGuess? {
        require(symbols.size == COMBINATION_SIZE)
        if (!isMainPhaseExhausted) return null
        if (hasBonusAttempt) return null
        if (isSinglePlayer || isOpponentDisconnected) return null
        val guess = SkockoGuess(symbols, evaluate(symbols))
        _bonusGuess = guess
        if (guess.hints.all { it == SkockoHint.CORRECT }) isSolvedByBonus = true
        return guess
    }

    override fun calculateScore(): Map<Player, Int> = SkockoScoringEngine.roundScore(
        mainPlayer = initialPlayer,
        isSolvedByMain = isSolvedByMain,
        mainAttemptsUsed = mainAttemptsUsed,
        isSolvedByBonus = isSolvedByBonus
    )

    private fun evaluate(symbols: List<SkockoSymbol>): List<SkockoHint> {
        val hints = Array(COMBINATION_SIZE) { SkockoHint.ABSENT }
        val secretUsed = BooleanArray(COMBINATION_SIZE)
        val guessUsed = BooleanArray(COMBINATION_SIZE)

        // Pass 1: exact position matches
        for (i in 0 until COMBINATION_SIZE) {
            if (symbols[i] == secret[i]) {
                hints[i] = SkockoHint.CORRECT
                secretUsed[i] = true
                guessUsed[i] = true
            }
        }

        // Pass 2: wrong-position matches
        for (i in 0 until COMBINATION_SIZE) {
            if (guessUsed[i]) continue
            for (j in 0 until COMBINATION_SIZE) {
                if (secretUsed[j]) continue
                if (symbols[i] == secret[j]) {
                    hints[i] = SkockoHint.PRESENT
                    secretUsed[j] = true
                    break
                }
            }
        }

        return hints.toList()
    }
}
