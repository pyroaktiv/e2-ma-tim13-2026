package rs.tim13.slagalica.skocko.model

import rs.tim13.slagalica.core.model.GameStatistics

/**
 * Statistika Skočka (spec 2.c): procenat pogođene kombinacije po pokušaju, po igraču.
 *
 * [blueCorrectAtAttempt] / [redCorrectAtAttempt] su dužine 6 (po jedan brojač za
 * svaki od 6 pokušaja); [blueFailed] / [redFailed] broje runde u kojima igrač nije pogodio.
 */
data class SkockoStatistics(
    val blueCorrectAtAttempt: List<Int>,
    val blueFailed: Int,
    val redCorrectAtAttempt: List<Int>,
    val redFailed: Int
) : GameStatistics
