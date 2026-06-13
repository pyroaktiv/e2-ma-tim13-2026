package rs.tim13.slagalica.notifications.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rs.tim13.slagalica.R
import rs.tim13.slagalica.auth.data.api.NotificationDto
import rs.tim13.slagalica.core.network.RetrofitClient
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentNotificationsBinding
import rs.tim13.slagalica.notifications.model.NotificationCategory
import rs.tim13.slagalica.notifications.model.NotificationModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationsFragment :
    BaseFragment<FragmentNotificationsBinding>(FragmentNotificationsBinding::inflate) {

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupChips()
        observeViewModel()
        loadNotifications()
    }

    private fun api() = RetrofitClient.getAuthClient(requireContext())

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(onMarkAsRead = { id -> markAsRead(id) })
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter
    }

    private fun setupChips() {
        binding.chipAll.setOnClickListener { viewModel.setFilter(NotificationFilter.ALL) }
        binding.chipUnread.setOnClickListener { viewModel.setFilter(NotificationFilter.UNREAD) }
        binding.chipRead.setOnClickListener { viewModel.setFilter(NotificationFilter.READ) }
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.tvUnreadCount.text = getString(R.string.notification_unread_count, count)
                binding.tvUnreadCount.visibility = View.VISIBLE
            } else {
                binding.tvUnreadCount.visibility = View.GONE
            }
        }
    }

    private fun loadNotifications() {
        api().getNotifications().enqueue(object : Callback<List<NotificationDto>> {
            override fun onResponse(call: Call<List<NotificationDto>>, response: Response<List<NotificationDto>>) {
                if (view == null) return
                val list = response.body()?.map { it.toModel() } ?: emptyList()
                viewModel.setNotifications(list)
            }

            override fun onFailure(call: Call<List<NotificationDto>>, t: Throwable) {
                if (isAdded) showError("Obaveštenja nisu učitana: ${t.message}")
            }
        })
    }

    private fun markAsRead(id: Long) {
        api().markNotificationRead(id).enqueue(object : Callback<rs.tim13.slagalica.auth.data.api.MessageResponse> {
            override fun onResponse(
                call: Call<rs.tim13.slagalica.auth.data.api.MessageResponse>,
                response: Response<rs.tim13.slagalica.auth.data.api.MessageResponse>,
            ) {
                if (view != null) loadNotifications()
            }

            override fun onFailure(call: Call<rs.tim13.slagalica.auth.data.api.MessageResponse>, t: Throwable) {}
        })
    }

    private fun NotificationDto.toModel(): NotificationModel {
        val category = try {
            NotificationCategory.valueOf(category)
        } catch (_: Exception) {
            NotificationCategory.OSTALO
        }
        val time = try {
            Instant.parse(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } catch (_: Exception) {
            LocalDateTime.now()
        }
        return NotificationModel(id, category, title, body, time, isRead)
    }
}
