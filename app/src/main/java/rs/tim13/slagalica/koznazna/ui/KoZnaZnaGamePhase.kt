package rs.tim13.slagalica.koznazna.ui

import rs.tim13.slagalica.core.ui.GamePhase

enum class KoZnaZnaGamePhase : GamePhase {
    PLAYING,    // igrači odgovaraju na tekuće pitanje
    REVEAL,     // prikazan tačan odgovor pre prelaska na sledeće
    GAME_OVER
}
