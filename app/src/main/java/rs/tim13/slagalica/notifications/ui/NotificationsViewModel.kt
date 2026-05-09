package rs.tim13.slagalica.notifications.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import rs.tim13.slagalica.notifications.data.MockNotificationRepository
import rs.tim13.slagalica.notifications.data.NotificationRepository
import rs.tim13.slagalica.notifications.model.NotificationModel

enum class NotificationFilter { ALL, UNREAD, READ }

class NotificationsViewModel(
    private val repository: NotificationRepository = MockNotificationRepository
) : ViewModel() {

    private val _filter = MutableLiveData(NotificationFilter.ALL)

    private val _notifications = MutableLiveData<List<NotificationModel>>()
    val notifications: LiveData<List<NotificationModel>> = _notifications

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    init {
        loadNotifications()
    }

    fun setFilter(filter: NotificationFilter) {
        _filter.value = filter
        applyFilter()
    }

    fun markAsRead(notificationId: Long) {
        repository.markAsRead(notificationId)
        loadNotifications()
    }

    private fun loadNotifications() {
        val all = repository.getAllNotifications()
        _unreadCount.value = all.count { !it.isRead }
        applyFilter()
    }

    private fun applyFilter() {
        val all = repository.getAllNotifications()
        _notifications.value = when (_filter.value) {
            NotificationFilter.UNREAD -> all.filter { !it.isRead }
            NotificationFilter.READ -> all.filter { it.isRead }
            else -> all
        }.sortedByDescending { it.timestamp }
    }
}
