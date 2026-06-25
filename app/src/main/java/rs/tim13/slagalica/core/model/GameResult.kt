package rs.tim13.slagalica.core.model

/**
 * Jedinstveni rezultat jedne igre koji koordinator partije konzumira.
 *
 * - [blueScore] / [redScore]: bodovi koje je svaki igrač osvojio u igri; koriste se
 *   za obračun pobednika, zvezda i tokena u 3. Igranje partija.
 * - [statistics]: podaci specifični za igru, za 2.c statistiku profila.
 *
 * Sve igre završavaju jednako — emitujući [rs.tim13.slagalica.core.ui.GameEvent.GameFinished]
 * sa ovim rezultatom — pa koordinator ne mora da poznaje konkretnu igru da bi sabrao skor.
 */
data class GameResult(
    val blueScore: Int,
    val redScore: Int,
    val statistics: GameStatistics
)
