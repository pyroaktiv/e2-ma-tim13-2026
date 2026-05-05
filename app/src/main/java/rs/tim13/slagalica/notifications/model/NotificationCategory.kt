package rs.tim13.slagalica.notifications.model

import androidx.annotation.StringRes
import rs.tim13.slagalica.R

enum class NotificationCategory(val channelId: String, @StringRes val labelRes: Int) {
    CET("notification_channel_cet", R.string.notification_channel_cet),
    RANGIRANJE("notification_channel_rangiranje", R.string.notification_channel_rangiranje),
    NAGRADE("notification_channel_nagrade", R.string.notification_channel_nagrade),
    OSTALO("notification_channel_ostalo", R.string.notification_channel_ostalo)
}
