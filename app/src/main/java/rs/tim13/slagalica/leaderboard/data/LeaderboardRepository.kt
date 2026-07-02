package rs.tim13.slagalica.leaderboard.data

import rs.tim13.slagalica.leaderboard.model.LeaderboardResponse

interface LeaderboardRepository {
    fun getWeekly(callback: (LeaderboardResponse?, String?) -> Unit)
    fun getMonthly(callback: (LeaderboardResponse?, String?) -> Unit)
}
