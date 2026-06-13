package rs.tim13.slagalica.core.model

abstract class BaseGame(
    val isSinglePlayer: Boolean,
    initialOpponentDisconnected: Boolean = false
) {
    var isOpponentDisconnected: Boolean = initialOpponentDisconnected
        protected set

    abstract fun calculateScore(): Map<Player, Int>

    open fun handleOpponentDisconnect() {
        isOpponentDisconnected = true
    }
}