package rs.tim13.slagalica.chat.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.R
import rs.tim13.slagalica.chat.data.dto.ConversationDto
import rs.tim13.slagalica.databinding.ItemConversationBinding

class ChatListAdapter(
    private val onClick: (ConversationDto) -> Unit
) : ListAdapter<ConversationDto, ChatListAdapter.ConversationViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ConversationDto>() {
            override fun areItemsTheSame(oldItem: ConversationDto, newItem: ConversationDto) =
                oldItem.userId == newItem.userId
            override fun areContentsTheSame(oldItem: ConversationDto, newItem: ConversationDto) = oldItem == newItem
        }
    }

    inner class ConversationViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ConversationDto) {
            val context = binding.root.context
            binding.tvConversationUsername.text = item.username
            binding.tvConversationLastMessage.text = item.lastMessage?.let { body ->
                if (item.lastMessageMine) {
                    context.getString(R.string.chat_last_message_format, context.getString(R.string.chat_prefix_me), body)
                } else {
                    body
                }
            }.orEmpty()
            binding.viewOnlineDot.visibility = if (item.isOnline) View.VISIBLE else View.GONE
            binding.tvUnreadBadge.visibility = if (item.unreadCount > 0) View.VISIBLE else View.GONE
            binding.tvUnreadBadge.text = item.unreadCount.toString()
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
