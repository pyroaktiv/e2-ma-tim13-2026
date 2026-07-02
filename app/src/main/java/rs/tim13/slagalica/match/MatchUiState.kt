package rs.tim13.slagalica.match

import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.network.socket.ChallengeResultEntryDto
import rs.tim13.slagalica.core.network.socket.MatchRewards
import rs.tim13.slagalica.core.network.socket.TurnirRewardsDto

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

    /** Izazov (spec 9): lokalni rezultat je prijavljen, čeka se da i ostali učesnici završe. */
    data object ChallengeWaitingForOthers : MatchUiState()

    /** Izazov je završen — konačan plasman i nagrade svih učesnika. */
    data class ChallengeFinished(val results: List<ChallengeResultEntryDto>) : MatchUiState()

    // Turnir (spec 10)

    /** Turnirska partija završena, čeka se `tournament_semi_over` ili `tournament_over`. */
    data object TournamentWaiting : MatchUiState()

    /** Polufinale završeno — prikaži rezultat i nagrade. */
    data class TournamentSemiResult(
        val won: Boolean,
        val myScore: Int,
        val opponentScore: Int,
        val opponentUsername: String,
        val rewards: TurnirRewardsDto?
    ) : MatchUiState()

    /** Finale završeno — prikaži konačan rezultat i nagrade. */
    data class TournamentFinalResult(
        val amIWinner: Boolean,
        val myScore: Int,
        val opponentScore: Int,
        val opponentUsername: String,
        val myRewards: TurnirRewardsDto
    ) : MatchUiState()
}
