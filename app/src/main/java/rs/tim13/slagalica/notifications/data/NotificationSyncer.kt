package rs.tim13.slagalica.notifications.data

import android.content.Context
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.core.NotificationHelper
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.leaderboard.model.NotificationDto
import rs.tim13.slagalica.notifications.model.NotificationCategory

object NotificationSyncer {

    private const val PREFS_NAME = "notification_sync"
    private const val KEY_SHOWN_IDS = "shown_ids"

    fun sync(context: Context) {
        RetrofitClient.getApiService(context).getNotifications()
            .enqueue(object : Callback<List<NotificationDto>> {
                override fun onResponse(
                    call: Call<List<NotificationDto>>,
                    response: Response<List<NotificationDto>>
                ) {
                    val notifications = response.body() ?: return
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val shownIds = prefs.getStringSet(KEY_SHOWN_IDS, emptySet())!!.toMutableSet()

                    notifications
                        .filter { !it.isRead && it.id.toString() !in shownIds }
                        .forEach { dto ->
                            val category = NotificationCategory.entries
                                .find { it.name == dto.category }
                                ?: NotificationCategory.OSTALO
                            NotificationHelper.sendNotification(context, category, dto.title, dto.body)
                            shownIds.add(dto.id.toString())
                        }

                    prefs.edit().putStringSet(KEY_SHOWN_IDS, shownIds).apply()
                }

                override fun onFailure(call: Call<List<NotificationDto>>, t: Throwable) {}
            })
    }
}
