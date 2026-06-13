package rs.tim13.slagalica.core.ui

abstract class GameEvent {
    data class MovePlayed(val action: String, val payload: Any) : GameEvent()

    abstract class GameFinished(
        open val totalBlueScore: Int,
        open val totalRedScore: Int
    ) : GameEvent()
}
