package rs.tim13.slagalica.skocko.model

import rs.tim13.slagalica.core.model.Player

object SkockoScoringEngine {

    fun roundScore(
        mainPlayer: Player,
        isSolvedByMain: Boolean,
        mainAttemptsUsed: Int,
        isSolvedByBonus: Boolean,
        bonusPlayer: Player? = null
    ): Map<Player, Int> {
        val scores = mutableMapOf(Player.BLUE to 0, Player.RED to 0)
        val opponent = Player.entries.first { it != mainPlayer }

        if (isSolvedByMain) {
            scores[mainPlayer] = when {
                mainAttemptsUsed <= 2 -> 20
                mainAttemptsUsed <= 4 -> 15
                else -> 10
            }
        } else if (isSolvedByBonus) {
            scores[bonusPlayer ?: opponent] = 10
        }

        return scores
    }
}
