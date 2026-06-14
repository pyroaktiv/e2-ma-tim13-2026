package rs.tim13.slagalica.match

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.network.socket.MatchRewards

/**
 * Stanje partije koje host (MatchHostFragment) prikazuje. Igre se prikazuju kao dečji
 * fragmenti u fazi [PlayingGame]; ostale faze su prekrivni ekrani (traženje, međurezultat, kraj).
 */
sealed class MatchUiState {

    /** Povezivanje na server (online), pre traženja protivnika. */
    data object Connecting : MatchUiState()

    /** Traženje protivnika (online matchmaking). */
    data object Searching : MatchUiState()

    /** Trenutno se igra [game]; host prikazuje odgovarajući dečji fragment. */
    data class PlayingGame(
        val game: MatchGame,
        val blueTotal: Int,
        val redTotal: Int,
        val localColor: Player
    ) : MatchUiState()

    /** Kratka tabela rezultata između dve igre (2s). */
    data class InterGame(
        val finished: MatchGame,
        val next: MatchGame?,
        val blueTotal: Int,
        val redTotal: Int,
        val localColor: Player
    ) : MatchUiState()

    /** Kraj partije: ukupan skor, nagrade (null za solo/gosta), da li je protivnik napustio. */
    data class MatchOver(
        val blueTotal: Int,
        val redTotal: Int,
        val localColor: Player,
        val rewards: MatchRewards?,
        val opponentLeft: Boolean
    ) : MatchUiState()

    /** Greška (npr. nema dovoljno tokena za partiju). */
    data class Error(val message: String) : MatchUiState()
}
