package rs.tim13.slagalica.core.model

abstract class TurnBasedGame(
    val initialPlayer: Player,
    isSinglePlayer: Boolean,
    initialOpponentDisconnected: Boolean = false
) : BaseGame(isSinglePlayer, initialOpponentDisconnected) {

    var activePlayer: Player = initialPlayer
        protected set

    protected open fun switchPlayer() {
        activePlayer = Player.entries.first { it != activePlayer }
    }

    fun handleOpponentDisconnect(remainingPlayer: Player) {
        super.handleOpponentDisconnect()
        activePlayer = remainingPlayer
    }
}