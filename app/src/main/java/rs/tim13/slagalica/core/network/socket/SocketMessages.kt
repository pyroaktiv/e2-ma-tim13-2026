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

    // Izazov (spec 9)
    data class CreateChallenge(
        val type: String = "create_challenge",
        val stars: Int,
        val tokens: Int
    ) : ClientMessage()
    data class JoinChallenge(val type: String = "join_challenge", val challengeId: String) : ClientMessage()
    data class LeaveChallenge(val type: String = "leave_challenge", val challengeId: String) : ClientMessage()
    data class StartChallenge(val type: String = "start_challenge", val challengeId: String) : ClientMessage()
    data class ReportChallengeResult(
        val type: String = "report_challenge_result",
        val challengeId: String,
        val score: Int,
        val perGame: List<PerGameStatsDto>
    ) : ClientMessage()

    // Čet (spec 8)
    data class SendChatMessage(
        val type: String = "chat_send",
        val toUserId: Int,
        val body: String
    ) : ClientMessage()
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

/** Učesnik izazova (spec 9); `score`/nagrade su null dok izazov traje. */
data class ChallengeParticipantDto(
    val userId: Int,
    val username: String,
    val score: Int?,
    val rewardStars: Int?,
    val rewardTokens: Int?
)

/** Stanje jednog izazova — isti oblik za REST listu i za `challenge_update` poruku. */
data class ChallengeDto(
    val id: String,
    val creatorId: Int,
    val creatorUsername: String,
    val stakeStars: Int,
    val stakeTokens: Int,
    val status: String, // "open" | "active" | "finished" | "cancelled"
    val participants: List<ChallengeParticipantDto>
)

/** Konačan plasman jednog učesnika nakon završetka izazova. */
data class ChallengeResultEntryDto(
    val userId: Int,
    val username: String,
    val score: Int,
    val rewardStars: Int,
    val rewardTokens: Int
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

    // Izazov (spec 9)
    data class ChallengeCreated(val challengeId: String) : ServerMessage()
    data class ChallengeUpdate(val challenge: ChallengeDto) : ServerMessage()
    data class ChallengeCancelled(val challengeId: String) : ServerMessage()
    data class ChallengeStarted(val challengeId: String, val content: MatchContentDto) : ServerMessage()
    data class ChallengeOver(val challengeId: String, val results: List<ChallengeResultEntryDto>) : ServerMessage()

    // Čet (spec 8)
    data class ChatMessage(
        val id: Int,
        val fromUserId: Int,
        val fromUsername: String,
        val toUserId: Int,
        val body: String,
        val createdAt: String
    ) : ServerMessage()
}
