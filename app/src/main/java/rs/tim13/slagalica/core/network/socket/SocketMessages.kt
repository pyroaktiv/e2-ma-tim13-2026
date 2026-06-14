package rs.tim13.slagalica.core.network.socket

import rs.tim13.slagalica.core.model.GameStatistics

/**
 * Poruke koje klijent ŠALJE serveru preko socketa. `type` polje je deo žičanog formata
 * (Gson ga serijalizuje), pa server po njemu rutira poruku.
 */
sealed class ClientMessage {
    data class FindMatch(val type: String = "find_match", val mode: String = "random") : ClientMessage()
    data class CancelFind(val type: String = "cancel_find") : ClientMessage()
    data class MatchMove(
        val type: String = "match_move",
        val matchId: String,
        val gameIndex: Int,
        val action: String,
        val payload: Map<String, Any>
    ) : ClientMessage()
    data class ReportResult(
        val type: String = "report_result",
        val matchId: String,
        val blueScore: Int,
        val redScore: Int,
        val perGame: List<PerGameStatsDto>
    ) : ClientMessage()
    data class LeaveMatch(val type: String = "leave_match", val matchId: String) : ClientMessage()
}

/** Statistika jedne igre koja se prijavljuje serveru (spec 2.c). */
data class PerGameStatsDto(
    val game: String,                  // npr. "ko_zna_zna", "spojnice"...
    val statistics: GameStatistics
)

/** Nagrade koje server vrati registrovanom igraču na kraju partije; null za gosta. */
data class MatchRewards(
    val won: Boolean,
    val starsDelta: Int,
    val tokensDelta: Int,
    val totalStars: Int,
    val tokens: Int,
    val league: String
)

data class OpponentDto(
    val username: String,
    val guest: Boolean
)

/**
 * Poruke koje klijent PRIMA od servera. Parsiraju se ručno po `type` polju u [SocketManager].
 */
sealed class ServerMessage {
    /** Lokalni sentinel kojim se „čisti" poslednja vrednost LiveData pri novom povezivanju. */
    data object Idle : ServerMessage()

    data class MatchFound(
        val matchId: String,
        val color: String,             // "BLUE" ili "RED"
        val opponent: OpponentDto,
        val content: MatchContentDto
    ) : ServerMessage()

    data class RemoteMove(
        val gameIndex: Int,
        val action: String,
        val payload: Map<String, Any>
    ) : ServerMessage()

    data object OpponentLeft : ServerMessage()

    data class MatchOver(val rewards: MatchRewards?) : ServerMessage()

    data class ServerError(val message: String) : ServerMessage()
}
