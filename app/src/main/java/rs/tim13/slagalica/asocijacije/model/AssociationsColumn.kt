package rs.tim13.slagalica.asocijacije.model

import rs.tim13.slagalica.core.model.Player

data class AssociationsColumn(
    val index: Int,
    val fields: List<AssociationsField>,
    val solution: String,
    val isSolved: Boolean = false,
    val isSolutionRevealed: Boolean = false,
    val solvedBy: Player = Player.BLUE,
) {
    init {
        require(fields.size == 4);
        require(index in 0..3);
    }

    val label: String = listOf<String>("A","B","C","D")[index];

    val unrevealedCount: Int get() = fields.count { !(it.isRevealed && it.isRevealedByPlayer) }
    val hasRevealedField: Boolean get() = fields.any { it.isRevealed && it.isRevealedByPlayer }
}
