package rs.tim13.slagalica.koznazna.model

import rs.tim13.slagalica.core.model.Player

/**
 * Bodovanje jednog pitanja u „Ko zna zna" (spec 1):
 * tačan odgovor +10, netačan -5, a ako oba igrača odgovore tačno poene dobija brži.
 */
object KoZnaZnaScoringEngine {

    const val CORRECT_POINTS = 10
    const val WRONG_POINTS = -5

    fun questionScore(
        correctIndex: Int,
        blue: KoZnaZnaAnswer?,
        red: KoZnaZnaAnswer?
    ): Map<Player, Int> {
        val delta = mutableMapOf(Player.BLUE to 0, Player.RED to 0)

        val blueCorrect = blue != null && blue.optionIndex == correctIndex
        val redCorrect = red != null && red.optionIndex == correctIndex

        if (blueCorrect && redCorrect) {
            // Oba tačna -> poene dobija brži (kraće vreme reakcije); pri jednakosti deterministički plavi.
            val faster = if (blue!!.elapsedMs <= red!!.elapsedMs) Player.BLUE else Player.RED
            delta[faster] = CORRECT_POINTS
        } else {
            if (blue != null) delta[Player.BLUE] = if (blueCorrect) CORRECT_POINTS else WRONG_POINTS
            if (red != null) delta[Player.RED] = if (redCorrect) CORRECT_POINTS else WRONG_POINTS
        }

        return delta
    }
}
