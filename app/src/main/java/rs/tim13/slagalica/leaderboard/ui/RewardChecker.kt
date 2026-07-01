package rs.tim13.slagalica.leaderboard.ui

import android.content.Context
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.leaderboard.model.NotificationDto

object RewardChecker {

    fun check(context: Context, onReward: (NotificationDto) -> Unit) {
        RetrofitClient.getApiService(context).getNotifications()
            .enqueue(object : Callback<List<NotificationDto>> {
                override fun onResponse(
                    call: Call<List<NotificationDto>>,
                    response: Response<List<NotificationDto>>
                ) {
                    response.body()
                        ?.filter { !it.isRead && it.category == "NAGRADE" && it.title.contains("turnir", ignoreCase = true) }
                        ?.firstOrNull()
                        ?.let { onReward(it) }
                }
                override fun onFailure(call: Call<List<NotificationDto>>, t: Throwable) {}
            })
    }
}
