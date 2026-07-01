package rs.tim13.slagalica.korakpokorak.model

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.model.TurnBasedGame

/**
 * Logika jedne runde „Korak po korak". Vodeći igrač ([leadPlayer]) otkriva korak po korak
 * (jedan trag na svakih 10s) i ima po jedan pokušaj na svakom otkrivenom koraku. Ako ne
 * pogodi do kraja, protivnik u fazi popravka ([isRecoveryPhase]) dobija jedan pokušaj.
 */
class KorakPoKorakGame(
    val clues: List<String>,
    val solution: String,
    initialPlayer: Player,
    isSinglePlayer: Boolean = false,
    initialOpponentDisconnected: Boolean = false
) : TurnBasedGame(initialPlayer, isSinglePlayer, initialOpponentDisconnected) {

    init {
        require(clues.isNotEmpty() && clues.size <= MAX_STEPS)
    }

    val leadPlayer: Player = initialPlayer

    var revealedCount = 0
        private set
    var isSolved = false
        private set
    var solvedAtStep = 0
        private set
    var solvedInRecovery = false
        private set
    var solvedBy: Player? = null
        private set
    var isRecoveryPhase = false
        private set

    private var guessedThisStep = false

    /** Trenutni (poslednji otkriveni) korak, 1-indeksiran. */
    val currentStep: Int get() = revealedCount
    val revealedClues: List<String> get() = clues.take(revealedCount)
    val allStepsRevealed: Boolean get() = revealedCount >= clues.size

    /** Da li aktivni igrač sme da pogađa (jedan pokušaj po otkrivenom koraku). */
    val canGuess: Boolean get() = !isSolved && !guessedThisStep

    /** Otkriva sledeći korak. Vraća false ako su svi otkriveni. */
    fun revealNextStep(): Boolean {
        if (allStepsRevealed) return false
        revealedCount++
        guessedThisStep = false
        return true
    }

    /** Pokušaj aktivnog igrača na trenutnom koraku. Vraća true ako je tačno. */
    fun guess(attempt: String): Boolean {
        if (isSolved || guessedThisStep) return false
        guessedThisStep = true
        if (attempt.trim().equals(solution, ignoreCase = true)) {
            isSolved = true
            solvedAtStep = currentStep
            solvedInRecovery = isRecoveryPhase
            solvedBy = activePlayer
            return true
        }
        return false
    }

    fun canStartRecovery(): Boolean = !isSinglePlayer && !isOpponentDisconnected && !isSolved

    fun beginRecovery() {
        isRecoveryPhase = true
        guessedThisStep = false
        revealedCount = clues.size // protivnik vidi sve tragove
        if (activePlayer == leadPlayer) switchPlayer()
    }

    override fun calculateScore(): Map<Player, Int> =
        KorakPoKorakScoringEngine.roundScore(isSolved, solvedAtStep, solvedInRecovery, solvedBy)

    companion object {
        const val MAX_STEPS = 7
    }
}
