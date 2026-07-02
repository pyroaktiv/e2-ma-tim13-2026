package rs.tim13.slagalica.asocijacije.model

import rs.tim13.slagalica.core.model.Player

object ScoringEngine {

    private fun columnScore(column: AssociationsColumn) = 2 + column.unrevealedCount

    fun roundScore(columns: List<AssociationsColumn>, finalSolved: Boolean, solvedBy: Player): Map<Player, Int> {
        val scores = mutableMapOf(Player.BLUE to 0, Player.RED to 0)

        for (player in Player.entries) {
            val score = columns.filter { it.isSolved && it.solvedBy == player }.sumOf { columnScore(it) }
            scores[player] = score
        }

        if (finalSolved) {
            val untouchedBonus = columns.count { !it.isSolved && !it.hasRevealedField } * 6

            val touchedButUnsolvedPoints = columns
                .filter { !it.isSolved && it.hasRevealedField }
                .sumOf { columnScore(it) }

            val totalFinalBonus = 7 + untouchedBonus + touchedButUnsolvedPoints

            scores.compute(solvedBy) { _, currentScore -> (currentScore ?: 0) + totalFinalBonus }
        }

        return scores
    }
}