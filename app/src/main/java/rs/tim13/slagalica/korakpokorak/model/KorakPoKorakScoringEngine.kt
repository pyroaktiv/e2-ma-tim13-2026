package rs.tim13.slagalica.korakpokorak.model

import rs.tim13.slagalica.core.model.Player

/**
 * Bodovanje runde „Korak po korak" (spec 5):
 * pogodak na prvom koraku nosi [BASE_POINTS], a svaki naredni korak oduzima [STEP_PENALTY].
 * Ako vodeći igrač ne pogodi, protivnik u popravku osvaja [RECOVERY_POINTS].
 */
object KorakPoKorakScoringEngine {

    const val BASE_POINTS = 20
    const val STEP_PENALTY = 2
    const val RECOVERY_POINTS = 5

    fun roundScore(
        isSolved: Boolean,
        solvedAtStep: Int,
        solvedInRecovery: Boolean,
        solvedBy: Player?
    ): Map<Player, Int> {
        val scores = mutableMapOf(Player.BLUE to 0, Player.RED to 0)
        if (isSolved && solvedBy != null) {
            scores[solvedBy] = if (solvedInRecovery) {
                RECOVERY_POINTS
            } else {
                (BASE_POINTS - STEP_PENALTY * (solvedAtStep - 1)).coerceAtLeast(0)
            }
        }
        return scores
    }
}
