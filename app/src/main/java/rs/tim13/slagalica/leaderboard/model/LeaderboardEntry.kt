package rs.tim13.slagalica.leaderboard.model

import com.google.gson.annotations.SerializedName

data class LeaderboardEntry(
    @SerializedName("rank")        val rank: Int,
    @SerializedName("user_id")     val userId: Int,
    @SerializedName("username")    val username: String,
    @SerializedName("league_icon") val leagueIcon: String,
    @SerializedName("stars")       val stars: Int
)
