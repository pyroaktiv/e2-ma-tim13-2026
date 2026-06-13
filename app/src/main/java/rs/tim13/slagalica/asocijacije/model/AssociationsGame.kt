package rs.tim13.slagalica.asocijacije.model

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.model.TurnBasedGame

class AssociationsGame(
    columns: List<AssociationsColumn>,
    val finalSolution: String,
    initialPlayer: Player = Player.BLUE,
    isSinglePlayer: Boolean = false
) : TurnBasedGame(initialPlayer, isSinglePlayer) {

    init {
        require(columns.size == 4)
    }

    private val _columns: MutableList<AssociationsColumn> = columns.toMutableList()
    val columns: List<AssociationsColumn> get() = _columns.toList()

    var isFinalSolved: Boolean = false
        private set

    var solvedBy: Player = initialPlayer
        private set

    var isNextMoveRevealing: Boolean = true
        private set

    fun revealField(columnIndex: Int, fieldIndex: Int): Boolean {
        if (!isNextMoveRevealing) return false
        val col = _columns[columnIndex]
        if (col.isSolved || col.fields[fieldIndex].isRevealed) return false

        val fields = col.fields.toMutableList()
        fields[fieldIndex] = fields[fieldIndex].copy(isRevealed = true, isRevealedByPlayer = true)
        _columns[columnIndex] = col.copy(fields = fields)

        isNextMoveRevealing = false
        return true
    }

    private fun revealColumn(columnIndex: Int, isSolved: Boolean?, player: Player) {
        val col = _columns[columnIndex]
        _columns[columnIndex] = col.copy(
            fields = col.fields.map { it.copy(isRevealed = true) },
            isSolved = isSolved ?: col.isSolved,
            solvedBy = player,
            isSolutionRevealed = true
        )
    }

    fun guessColumn(columnIndex: Int, guess: String): Boolean {
        if (isNextMoveRevealing) return false
        val col = _columns[columnIndex]
        if (col.isSolved) return false

        return if (guess.trim().equals(col.solution, ignoreCase = true)) {
            revealColumn(columnIndex, true, activePlayer)
            true
        } else {
            if (!isSinglePlayer && !isOpponentDisconnected) switchPlayer()
            isNextMoveRevealing = true
            false
        }
    }

    fun guessFinal(guess: String): Boolean {
        if (isNextMoveRevealing) return false
        if (isFinalSolved) return false

        return if (guess.trim().equals(finalSolution, ignoreCase = true)) {
            isFinalSolved = true
            solvedBy = activePlayer
            _columns.indices.forEach { i -> revealColumn(i, null, activePlayer) }
            true
        } else {
            if (!isSinglePlayer && !isOpponentDisconnected) switchPlayer()
            isNextMoveRevealing = true
            false
        }
    }

    override fun calculateScore(): Map<Player, Int> = ScoringEngine.roundScore(_columns, isFinalSolved, solvedBy)
}