package rs.tim13.slagalica.notifications.data

import rs.tim13.slagalica.notifications.model.NotificationModel

interface NotificationRepository {
    suspend fun getAllNotifications(): List<NotificationModel>
    suspend fun markAsRead(notificationId: Long)
}
