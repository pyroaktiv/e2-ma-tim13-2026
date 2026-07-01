package rs.tim13.slagalica.notifications.data

import rs.tim13.slagalica.leaderboard.model.NotificationDto
import rs.tim13.slagalica.notifications.model.NotificationCategory
import rs.tim13.slagalica.notifications.model.NotificationModel
import java.time.LocalDateTime
import java.time.OffsetDateTime

/** Istorija notifikacija sa backenda (spec 11). Mapira wire [NotificationDto] u UI [NotificationModel]. */
class RemoteNotificationRepository(
    private val api: NotificationApiService
) : NotificationRepository {

    override suspend fun getAllNotifications(): List<NotificationModel> =
        api.getNotifications().map { it.toModel() }

    override suspend fun markAsRead(notificationId: Long) {
        api.markRead(notificationId)
    }

    private fun NotificationDto.toModel(): NotificationModel {
        val cat = runCatching { NotificationCategory.valueOf(category) }
            .getOrDefault(NotificationCategory.OSTALO)
        val ts = runCatching { OffsetDateTime.parse(timestamp).toLocalDateTime() }
            .getOrDefault(LocalDateTime.now())
        return NotificationModel(
            id = id,
            category = cat,
            title = title,
            body = body,
            timestamp = ts,
            isRead = isRead
        )
    }
}
