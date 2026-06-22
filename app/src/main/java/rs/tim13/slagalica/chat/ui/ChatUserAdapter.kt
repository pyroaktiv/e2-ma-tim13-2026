package rs.tim13.slagalica.chat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.chat.data.dto.ChatUserDto
import rs.tim13.slagalica.databinding.ItemChatUserBinding

class ChatUserAdapter(
    private val onClick: (ChatUserDto) -> Unit
) : ListAdapter<ChatUserDto, ChatUserAdapter.ChatUserViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatUserDto>() {
            override fun areItemsTheSame(oldItem: ChatUserDto, newItem: ChatUserDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChatUserDto, newItem: ChatUserDto) = oldItem == newItem
        }
    }

    inner class ChatUserViewHolder(private val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatUserDto) {
            binding.tvChatUserUsername.text = item.username
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatUserViewHolder {
        val binding = ItemChatUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
