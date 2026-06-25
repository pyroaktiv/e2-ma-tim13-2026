package rs.tim13.slagalica.spojnice.model

import rs.tim13.slagalica.core.model.GameStatistics

/**
 * Statistika Spojnica (spec 2.c): procenat uspešno povezanih pojmova, po igraču
 * (povezani parovi naspram ukupnih pokušaja).
 */
data class SpojniceStatistics(
    val blueConnected: Int,
    val blueAttempts: Int,
    val redConnected: Int,
    val redAttempts: Int
) : GameStatistics
