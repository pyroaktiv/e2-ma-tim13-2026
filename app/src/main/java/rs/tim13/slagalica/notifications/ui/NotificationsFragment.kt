package rs.tim13.slagalica.notifications.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentNotificationsBinding

class NotificationsFragment :
    BaseFragment<FragmentNotificationsBinding>(FragmentNotificationsBinding::inflate) {

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupChips()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(onMarkAsRead = { id -> viewModel.markAsRead(id) })
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
                binding.tvUnreadCount.text =
                    getString(R.string.notification_unread_count, count)
                binding.tvUnreadCount.visibility = View.VISIBLE
            } else {
                binding.tvUnreadCount.visibility = View.GONE
            }
        }
    }
}
