package rs.tim13.slagalica.notifications.model

import java.time.LocalDateTime

data class NotificationModel(
    val id: Long,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val timestamp: LocalDateTime,
    val isRead: Boolean
)
