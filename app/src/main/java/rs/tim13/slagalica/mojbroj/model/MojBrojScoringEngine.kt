package rs.tim13.slagalica.mojbroj.model

import rs.tim13.slagalica.core.model.Player
import kotlin.math.abs

/**
 * Bodovanje jedne runde „Moj broj" (spec 6, g–j).
 *
 * - Ko dobije tačan broj: +10.
 * - Ako niko nema tačan broj: 5 bodova bližem rezultatu; ko ništa nije uneo (null) dobija 0.
 * - Ako su rezultati jednako udaljeni (uklj. isti rezultat ≠ tačan): 5 bodova igraču čija je runda.
 */
object MojBrojScoringEngine {

    const val EXACT_POINTS = 10
    const val CLOSEST_POINTS = 5

    fun roundScore(target: Int, blue: Int?, red: Int?, roundLeader: Player): Map<Player, Int> {
        val scores = mutableMapOf(Player.BLUE to 0, Player.RED to 0)

        val blueExact = blue == target
        val redExact = red == target
        if (blueExact || redExact) {
            if (blueExact) scores[Player.BLUE] = EXACT_POINTS
            if (redExact) scores[Player.RED] = EXACT_POINTS
            return scores
        }

        // Niko nije pogodio: bliži rezultat nosi 5 bodova.
        val blueDistance = blue?.let { abs(it - target) }
        val redDistance = red?.let { abs(it - target) }

        when {
            blueDistance == null && redDistance == null -> Unit // niko ništa nije uneo
            blueDistance == null -> scores[Player.RED] = CLOSEST_POINTS
            redDistance == null -> scores[Player.BLUE] = CLOSEST_POINTS
            blueDistance < redDistance -> scores[Player.BLUE] = CLOSEST_POINTS
            redDistance < blueDistance -> scores[Player.RED] = CLOSEST_POINTS
            else -> scores[roundLeader] = CLOSEST_POINTS // jednaka udaljenost -> igrač čija je runda
        }

        return scores
    }
}
