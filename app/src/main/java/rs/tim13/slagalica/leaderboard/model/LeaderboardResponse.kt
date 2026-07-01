package rs.tim13.slagalica.leaderboard.model

import com.google.gson.annotations.SerializedName

data class LeaderboardResponse(
    @SerializedName("cycle_start") val cycleStart: String,
    @SerializedName("cycle_end")   val cycleEnd: String,
    @SerializedName("entries")     val entries: List<LeaderboardEntry>,
    @SerializedName("my_rank")     val myRank: Int?
)
