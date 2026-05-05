package rs.tim13.slagalica.asocijacije.model

data class AssociationsField(
    val text: String,
    val isRevealed: Boolean = false,
    val isRevealedByPlayer: Boolean = false,
)
