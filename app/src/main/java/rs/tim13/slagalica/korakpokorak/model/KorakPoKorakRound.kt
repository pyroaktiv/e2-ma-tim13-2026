package rs.tim13.slagalica.korakpokorak.model

/**
 * Podaci jedne runde „Korak po korak": tragovi (od najtežeg ka najlakšem) i tačno [solution].
 */
data class KorakPoKorakRound(
    val clues: List<String>,
    val solution: String
) {
    init {
        require(clues.isNotEmpty() && clues.size <= KorakPoKorakGame.MAX_STEPS)
    }
}
