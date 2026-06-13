package rs.tim13.slagalica.notifications.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import rs.tim13.slagalica.notifications.model.NotificationModel

enum class NotificationFilter { ALL, UNREAD, READ }

class NotificationsViewModel : ViewModel() {

    private var master: List<NotificationModel> = emptyList()
    private var filter = NotificationFilter.ALL

    private val _notifications = MutableLiveData<List<NotificationModel>>(emptyList())
    val notifications: LiveData<List<NotificationModel>> = _notifications

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    /** Called by the fragment after loading notifications from the backend. */
    fun setNotifications(list: List<NotificationModel>) {
        master = list
        _unreadCount.value = list.count { !it.isRead }
        applyFilter()
    }

    fun setFilter(filter: NotificationFilter) {
        this.filter = filter
        applyFilter()
    }

    private fun applyFilter() {
        _notifications.value = when (filter) {
            NotificationFilter.UNREAD -> master.filter { !it.isRead }
            NotificationFilter.READ -> master.filter { it.isRead }
            else -> master
        }.sortedByDescending { it.timestamp }
    }
}
