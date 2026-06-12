package rs.tim13.slagalica.notifications.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.databinding.ItemNotificationBinding
import rs.tim13.slagalica.notifications.model.NotificationModel
import java.time.format.DateTimeFormatter

class NotificationAdapter(
    private val onMarkAsRead: (Long) -> Unit
) : ListAdapter<NotificationModel, NotificationAdapter.NotificationViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotificationModel>() {
            override fun areItemsTheSame(oldItem: NotificationModel, newItem: NotificationModel) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NotificationModel, newItem: NotificationModel) =
                oldItem == newItem
        }

        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationModel) {
            binding.tvNotificationTitle.text = item.title
            binding.tvNotificationBody.text = item.body
            binding.tvNotificationTimestamp.text = item.timestamp.format(TIME_FORMATTER)
            binding.tvNotificationChannel.text = binding.root.context.getString(item.category.labelRes)

            binding.root.alpha = if (item.isRead) 0.65f else 1.0f
            binding.viewUnreadIndicator.visibility =
                if (item.isRead) View.INVISIBLE else View.VISIBLE
            binding.btnMarkAsRead.visibility =
                if (item.isRead) View.GONE else View.VISIBLE

            binding.btnMarkAsRead.setOnClickListener { onMarkAsRead(item.id) }
            // tapping the notification also marks it read (react to it)
            binding.root.setOnClickListener { if (!item.isRead) onMarkAsRead(item.id) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
