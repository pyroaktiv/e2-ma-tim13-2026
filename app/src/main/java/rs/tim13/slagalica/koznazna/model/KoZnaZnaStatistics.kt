package rs.tim13.slagalica.koznazna.model

import rs.tim13.slagalica.core.model.GameStatistics

/**
 * Statistika „Ko zna zna" (spec 2.c): odnos pogođenih i promašenih pitanja, po igraču.
 */
data class KoZnaZnaStatistics(
    val blueCorrect: Int,
    val blueWrong: Int,
    val redCorrect: Int,
    val redWrong: Int
) : GameStatistics
