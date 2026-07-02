package rs.tim13.slagalica.core

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import rs.tim13.slagalica.R
import rs.tim13.slagalica.notifications.model.NotificationCategory

object NotificationHelper {

    private var nextId = 100

    /**
     * Prikazuje sistemsku notifikaciju na kanalu date kategorije (spec 11). Samo prikaz —
     * istorija se čuva na backendu (vidi backend `createNotification`), pa je ovde ne diramo.
     * Koristi je FCM servis kada stigne push dok app nije u prvom planu.
     */
    fun show(
        context: Context,
        category: NotificationCategory,
        title: String,
        body: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, category.channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(nextId++, notification)
    }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channels = listOf(
            buildChannel(
                id = NotificationCategory.CET.channelId,
                name = context.getString(R.string.notification_channel_cet),
                desc = context.getString(R.string.notification_channel_cet_desc),
                importance = NotificationManager.IMPORTANCE_DEFAULT
            ),
            buildChannel(
                id = NotificationCategory.RANGIRANJE.channelId,
                name = context.getString(R.string.notification_channel_rangiranje),
                desc = context.getString(R.string.notification_channel_rangiranje_desc),
                importance = NotificationManager.IMPORTANCE_HIGH
            ),
            buildChannel(
                id = NotificationCategory.NAGRADE.channelId,
                name = context.getString(R.string.notification_channel_nagrade),
                desc = context.getString(R.string.notification_channel_nagrade_desc),
                importance = NotificationManager.IMPORTANCE_DEFAULT
            ),
            buildChannel(
                id = NotificationCategory.OSTALO.channelId,
                name = context.getString(R.string.notification_channel_ostalo),
                desc = context.getString(R.string.notification_channel_ostalo_desc),
                importance = NotificationManager.IMPORTANCE_LOW
            )
        )

        manager.createNotificationChannels(channels)
    }

    private fun buildChannel(
        id: String,
        name: String,
        desc: String,
        importance: Int
    ): android.app.NotificationChannel {
        return android.app.NotificationChannel(id, name, importance).apply {
            description = desc
        }
    }
}
