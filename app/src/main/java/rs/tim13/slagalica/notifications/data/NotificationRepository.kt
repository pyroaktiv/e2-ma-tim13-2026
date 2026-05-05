package rs.tim13.slagalica.notifications.data

import rs.tim13.slagalica.notifications.model.NotificationModel

interface NotificationRepository {
    fun getAllNotifications(): List<NotificationModel>
    fun markAsRead(notificationId: Long)
}
