package rs.tim13.slagalica.notifications.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rs.tim13.slagalica.notifications.data.NotificationRepository
import rs.tim13.slagalica.notifications.model.NotificationModel

enum class NotificationFilter { ALL, UNREAD, READ }

/** Istorija notifikacija (spec 11.b/c/d): učitava sa backenda, filtrira i označava pročitano. */
class NotificationsViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private var _filter = NotificationFilter.ALL
    private var cache: List<NotificationModel> = emptyList()

    private val _notifications = MutableLiveData<List<NotificationModel>>(emptyList())
    val notifications: LiveData<List<NotificationModel>> = _notifications

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            cache = runCatching { repository.getAllNotifications() }.getOrDefault(cache)
            _unreadCount.value = cache.count { !it.isRead }
            applyFilter()
        }
    }

    fun setFilter(filter: NotificationFilter) {
        _filter = filter
        applyFilter()
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            runCatching { repository.markAsRead(notificationId) }
            refresh()
        }
    }

    private fun applyFilter() {
        _notifications.value = when (_filter) {
            NotificationFilter.UNREAD -> cache.filter { !it.isRead }
            NotificationFilter.READ -> cache.filter { it.isRead }
            NotificationFilter.ALL -> cache
        }.sortedByDescending { it.timestamp }
    }
}
