package rs.tim13.slagalica.skocko.ui

import rs.tim13.slagalica.core.ui.GamePhase

enum class SkockoGamePhase : GamePhase {
    MAIN_TURN,
    BONUS_TURN,
    ROUND_OVER,
    GAME_OVER
}