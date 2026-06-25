package rs.tim13.slagalica.mojbroj.model

/**
 * Zagonetka jedne runde „Moj broj": traženi [target] i šest ponuđenih [numbers]
 * (četiri jednocifrena, jedan iz {10,15,20} i jedan iz {25,50,75,100}).
 */
data class MojBrojRound(
    val target: Int,
    val numbers: List<Int>
) {
    init {
        require(numbers.size == MojBrojGame.NUMBER_COUNT)
    }
}
