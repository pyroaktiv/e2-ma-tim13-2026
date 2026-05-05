package rs.tim13.slagalica.asocijacije.model

import rs.tim13.slagalica.core.Player

object ScoringEngine {

    private fun columnScore(column: AssociationsColumn) = 2 + column.unrevealedCount

    //Per spec:
    //  finalSolved -> 7 + 6*(untouched columns) + columnScore for every touched column
    // !finalSolved -> sum of columnScore for solved columns only
    private fun _roundScore(columns: List<AssociationsColumn>, finalSolved: Boolean): Int {
        if (!finalSolved) {
            return columns.filter { it.isSolved }.sumOf { columnScore(it) }
        }
        val untouchedBonus = columns.count { !it.isSolved && !it.hasRevealedField } * 6
        val touchedPoints = columns
            .filter { it.isSolved || it.hasRevealedField }
            .sumOf { columnScore(it) }
        return 7 + untouchedBonus + touchedPoints
    }

    fun roundScore(columns: List<AssociationsColumn>, finalSolved: Boolean, solvedBy: Player): Map<Player, Int> {
        val scores = mutableMapOf(Player.BLUE to 0, Player.RED to 0)

        if (finalSolved) {
            val untouchedBonus = columns.count { !it.isSolved && !it.hasRevealedField } * 6
            val totalBonus = untouchedBonus + 7

            scores.compute(solvedBy) {
                _, v -> (v ?: 0) + totalBonus
            }
        }

        for(player in Player.entries) {
            val score = columns.filter { it.isSolved && it.solvedBy == player }.sumOf { columnScore(it) }
            scores.compute(player) {
                _, v -> (v ?: 0) + score
            }
        }

        return scores
    }
}