package rs.tim13.slagalica.auth.data.api

import com.google.gson.annotations.SerializedName

// Request / response DTOs for the REST endpoints exposed by the backend.

data class LoginRequest(
    val identifier: String,
    val password: String,
)

data class LoginResponse(
    val token: String,
    val user: UserDto,
)

data class UserDto(
    val id: Int,
    val username: String,
    val email: String,
    val avatar: String,
)

data class LeagueDto(
    val name: String,
    val icon: String,
)

data class ProfileResponse(
    val id: Int,
    val username: String,
    val email: String,
    val avatar: String,
    val tokens: Int,
    @SerializedName("total_stars") val totalStars: Int,
    val league: LeagueDto,
    val region: String,
    @SerializedName("qr_token") val qrToken: String,
)

data class OverallStats(
    @SerializedName("total_games") val totalGames: Int,
    val wins: Int,
    val losses: Int,
    @SerializedName("win_ratio") val winRatio: Double,
)

data class KoZnaZnaStats(val correct: Int, val missed: Int)
data class MojBrojStats(
    @SerializedName("total_attempts") val totalAttempts: Int,
    @SerializedName("exact_hits") val exactHits: Int,
)
data class KorakPoKorakStats(@SerializedName("guessed_at_step") val guessedAtStep: List<Int>)
data class AsocijacijeStats(val solved: Int, val unsolved: Int)
data class SkockoStats(
    @SerializedName("correct_at_attempt") val correctAtAttempt: List<Int>,
    val failed: Int,
)
data class SpojniceStats(val total: Int, val successful: Int)

data class StatsResponse(
    val overall: OverallStats,
    @SerializedName("ko_zna_zna") val koZnaZna: KoZnaZnaStats,
    @SerializedName("moj_broj") val mojBroj: MojBrojStats,
    @SerializedName("korak_po_korak") val korakPoKorak: KorakPoKorakStats,
    val asocijacije: AsocijacijeStats,
    val skocko: SkockoStats,
    val spojnice: SpojniceStats,
)
