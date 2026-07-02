package rs.tim13.slagalica.spojnice.model

/**
 * Podaci jedne runde Spojnica: pet pojmova levo, pet desno i tačno preslikavanje
 * [solution] (indeks levog -> indeks tačnog desnog).
 */
data class SpojniceRound(
    val leftItems: List<String>,
    val rightItems: List<String>,
    val solution: Map<Int, Int>
) {
    init {
        require(leftItems.size == SpojniceGame.PAIR_COUNT)
        require(rightItems.size == SpojniceGame.PAIR_COUNT)
        require(solution.size == SpojniceGame.PAIR_COUNT)
    }
}
