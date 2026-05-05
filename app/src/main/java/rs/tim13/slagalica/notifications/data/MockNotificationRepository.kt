package rs.tim13.slagalica.notifications.data

import rs.tim13.slagalica.notifications.model.NotificationCategory
import rs.tim13.slagalica.notifications.model.NotificationModel
import java.time.LocalDateTime

object MockNotificationRepository : NotificationRepository {

    private val _notifications: MutableList<NotificationModel> = mutableListOf(
        NotificationModel(
            id = 1L,
            category = NotificationCategory.RANGIRANJE,
            title = "Rangiranje ažurirano",
            body = "Zauzeli ste 3. mesto u Ligi 1 ove sedmice.",
            timestamp = LocalDateTime.now().minusHours(1),
            isRead = false
        ),
        NotificationModel(
            id = 2L,
            category = NotificationCategory.NAGRADE,
            title = "Nova nagrada!",
            body = "Dobili ste značku 'Majstor brojeva' za savršen rezultat u Moj Broj.",
            timestamp = LocalDateTime.now().minusHours(3),
            isRead = false
        ),
        NotificationModel(
            id = 3L,
            category = NotificationCategory.RANGIRANJE,
            title = "Unapređenje u ligi",
            body = "Čestitamo! Prešli ste u Ligu 2.",
            timestamp = LocalDateTime.now().minusDays(1),
            isRead = false
        ),
        NotificationModel(
            id = 4L,
            category = NotificationCategory.CET,
            title = "Poziv od prijatelja",
            body = "Ana te je pozvala da odigraš meč.",
            timestamp = LocalDateTime.now().minusDays(2),
            isRead = true
        ),
        NotificationModel(
            id = 5L,
            category = NotificationCategory.OSTALO,
            title = "Dobrodošli u Slagalicu!",
            body = "Istražite sve igre i popnite se na vrh rang liste.",
            timestamp = LocalDateTime.now().minusDays(3),
            isRead = true
        )
    )

    override fun getAllNotifications(): List<NotificationModel> = _notifications.toList()

    override fun markAsRead(notificationId: Long) {
        val index = _notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            _notifications[index] = _notifications[index].copy(isRead = true)
        }
    }
}
