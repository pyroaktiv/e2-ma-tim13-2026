package rs.tim13.slagalica.core.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import rs.tim13.slagalica.leaderboard.model.LeaderboardResponse
import rs.tim13.slagalica.leaderboard.model.NotificationDto

interface ApiService {

    @GET("api/leaderboard/weekly")
    fun getWeeklyLeaderboard(): Call<LeaderboardResponse>

    @GET("api/leaderboard/monthly")
    fun getMonthlyLeaderboard(): Call<LeaderboardResponse>

    @GET("api/notifications")
    fun getNotifications(): Call<List<NotificationDto>>

    @PATCH("api/notifications/{id}/read")
    fun markNotificationRead(@Path("id") id: Long): Call<Unit>
}
