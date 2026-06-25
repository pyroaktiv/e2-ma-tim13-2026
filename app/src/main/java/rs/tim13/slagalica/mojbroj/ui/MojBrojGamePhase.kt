package rs.tim13.slagalica.mojbroj.ui

import rs.tim13.slagalica.core.ui.GamePhase

enum class MojBrojGamePhase : GamePhase {
    SELECT_TARGET,   // klik na stop otkriva traženi broj
    SELECT_NUMBERS,  // klik na stop otkriva šest brojeva
    SOLVING,         // sastavljanje izraza (60s)
    ROUND_OVER,
    GAME_OVER
}
