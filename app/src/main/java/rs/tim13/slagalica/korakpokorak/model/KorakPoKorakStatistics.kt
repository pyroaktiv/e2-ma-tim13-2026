package rs.tim13.slagalica.korakpokorak.model

import rs.tim13.slagalica.core.model.GameStatistics

/**
 * Statistika „Korak po korak" (spec 2.c): procenat pogođenog pojma u svakom koraku, po igraču.
 *
 * [blueSolvedAtStep] / [redSolvedAtStep] su dužine [KorakPoKorakGame.MAX_STEPS] (broj pogodaka
 * po koraku kada je igrač vodio rundu); [blueFailed] / [redFailed] broje runde u kojima vodeći
 * igrač nije pogodio pojam.
 */
data class KorakPoKorakStatistics(
    val blueSolvedAtStep: List<Int>,
    val blueFailed: Int,
    val redSolvedAtStep: List<Int>,
    val redFailed: Int
) : GameStatistics
