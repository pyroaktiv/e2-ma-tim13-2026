package rs.tim13.slagalica.leaderboard.data

import android.content.Context
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.leaderboard.model.LeaderboardResponse

class LeaderboardRepositoryImpl(context: Context) : LeaderboardRepository {

    private val api = RetrofitClient.getApiService(context)

    override fun getWeekly(callback: (LeaderboardResponse?, String?) -> Unit) {
        api.getWeeklyLeaderboard().enqueue(object : Callback<LeaderboardResponse> {
            override fun onResponse(call: Call<LeaderboardResponse>, response: Response<LeaderboardResponse>) {
                if (response.isSuccessful) callback(response.body(), null)
                else callback(null, "Greška ${response.code()}")
            }
            override fun onFailure(call: Call<LeaderboardResponse>, t: Throwable) {
                callback(null, t.message ?: "Greška mreže")
            }
        })
    }

    override fun getMonthly(callback: (LeaderboardResponse?, String?) -> Unit) {
        api.getMonthlyLeaderboard().enqueue(object : Callback<LeaderboardResponse> {
            override fun onResponse(call: Call<LeaderboardResponse>, response: Response<LeaderboardResponse>) {
                if (response.isSuccessful) callback(response.body(), null)
                else callback(null, "Greška ${response.code()}")
            }
            override fun onFailure(call: Call<LeaderboardResponse>, t: Throwable) {
                callback(null, t.message ?: "Greška mreže")
            }
        })
    }
}
