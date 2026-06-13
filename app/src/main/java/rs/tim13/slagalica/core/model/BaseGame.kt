package rs.tim13.slagalica.core.model

abstract class BaseGame(
    val isSinglePlayer: Boolean
) {
    var isOpponentDisconnected: Boolean = false
        protected set

    abstract fun calculateScore(): Map<Player, Int>

    open fun handleOpponentDisconnect() {
        isOpponentDisconnected = true
    }
}