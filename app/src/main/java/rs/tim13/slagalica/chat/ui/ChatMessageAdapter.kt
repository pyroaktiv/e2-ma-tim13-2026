package rs.tim13.slagalica.chat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.chat.data.dto.ChatMessageDto
import rs.tim13.slagalica.databinding.ItemChatMessageInBinding
import rs.tim13.slagalica.databinding.ItemChatMessageOutBinding

/** Poruke od [otherUserId] se prikazuju levo, sopstvene desno (spec 8). */
class ChatMessageAdapter(
    private val otherUserId: Int
) : ListAdapter<ChatMessageDto, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val VIEW_TYPE_IN = 0
        private const val VIEW_TYPE_OUT = 1

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatMessageDto>() {
            override fun areItemsTheSame(oldItem: ChatMessageDto, newItem: ChatMessageDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatMessageDto, newItem: ChatMessageDto) = oldItem == newItem
        }
    }

    inner class InViewHolder(private val binding: ItemChatMessageInBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessageDto) {
            binding.tvMessageBody.text = item.body
        }
    }

    inner class OutViewHolder(private val binding: ItemChatMessageOutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessageDto) {
            binding.tvMessageBody.text = item.body
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).fromUserId == otherUserId) VIEW_TYPE_IN else VIEW_TYPE_OUT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_IN) {
            InViewHolder(ItemChatMessageInBinding.inflate(inflater, parent, false))
        } else {
            OutViewHolder(ItemChatMessageOutBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is InViewHolder -> holder.bind(getItem(position))
            is OutViewHolder -> holder.bind(getItem(position))
        }
    }
}
