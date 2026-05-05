package rs.tim13.slagalica.asocijacije.model

import rs.tim13.slagalica.core.Player

class AssociationsGame(
    columns: List<AssociationsColumn>,
    val finalSolution: String
) {
    init {
        require(columns.size == 4);
    }
    private val _columns: MutableList<AssociationsColumn> = columns.toMutableList()
    val columns: List<AssociationsColumn> get() = _columns.toList()

    var isFinalSolved: Boolean = false
        private set

    var solvedBy: Player = Player.BLUE
        private set

    fun revealField(columnIndex: Int, fieldIndex: Int): Boolean {
        val col = _columns[columnIndex]
        if (col.isSolved || col.fields[fieldIndex].isRevealed) return false
        val fields = col.fields.toMutableList()
        fields[fieldIndex] = fields[fieldIndex].copy(isRevealed = true, isRevealedByPlayer = true)
        _columns[columnIndex] = col.copy(fields = fields)
        return true
    }

    private fun revealColumn(columnIndex: Int, isSolved: Boolean?, player: Player) {
        val col = _columns[columnIndex]
        val fields = col.fields
        _columns[columnIndex] = col.copy(fields =
            fields.map {
                it.copy(isRevealed = true)
            },
            isSolved = isSolved ?: col.isSolved, solvedBy = player, isSolutionRevealed = true
        )
    }

    fun guessColumn(columnIndex: Int, guess: String, by: Player): Boolean {
        val col = _columns[columnIndex]
        if (col.isSolved) return false
        return if (guess.trim().equals(col.solution, ignoreCase = true)) {
            revealColumn(columnIndex, true, by)
            true
        } else false
    }

    fun guessFinal(guess: String, by: Player): Boolean {
        if (isFinalSolved) return false
        return if (guess.trim().equals(finalSolution, ignoreCase = true)) {
            isFinalSolved = true
            solvedBy = by
            _columns.indices.forEach { i -> revealColumn(i, null, by) }
            true
        } else false
    }

    fun calculateScore() = ScoringEngine.roundScore(_columns, isFinalSolved, solvedBy)

}
