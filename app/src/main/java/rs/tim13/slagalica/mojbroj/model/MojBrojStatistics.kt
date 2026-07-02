package rs.tim13.slagalica.mojbroj.model

import rs.tim13.slagalica.core.model.GameStatistics

/**
 * Statistika „Moj broj" (spec 2.c): procenat pronađenog tačnog broja, po igraču
 * (runde u kojima je pogođen tačan broj naspram odigranih rundi).
 */
data class MojBrojStatistics(
    val blueFound: Int,
    val bluePlayed: Int,
    val redFound: Int,
    val redPlayed: Int
) : GameStatistics
