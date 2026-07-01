package rs.tim13.slagalica.asocijacije.model

import rs.tim13.slagalica.core.model.GameStatistics

/**
 * Statistika Asocijacija (spec 2.c): odnos rešenih i nerešenih asocijacija, po igraču.
 */
data class AssociationsStatistics(
    val blueSolvedCount: Int,
    val blueUnsolvedCount: Int,
    val redSolvedCount: Int,
    val redUnsolvedCount: Int
) : GameStatistics
