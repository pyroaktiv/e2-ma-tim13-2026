package rs.tim13.slagalica.friends.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.core.util.ProfileResources
import rs.tim13.slagalica.databinding.ItemFriendSearchBinding
import rs.tim13.slagalica.friends.data.api.dto.SearchUserDto

/** Rezultati pretrage korisnika (spec 7.b). Dugme zavisi od relacije sa nađenim korisnikom. */
class FriendSearchAdapter(
    private val onAdd: (SearchUserDto) -> Unit
) : ListAdapter<SearchUserDto, FriendSearchAdapter.SearchViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SearchUserDto>() {
            override fun areItemsTheSame(oldItem: SearchUserDto, newItem: SearchUserDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: SearchUserDto, newItem: SearchUserDto) = oldItem == newItem
        }
    }

    inner class SearchViewHolder(private val binding: ItemFriendSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchUserDto) {
            binding.tvSearchUsername.text = item.username
            binding.ivSearchAvatar.setImageResource(ProfileResources.avatarDrawable(item.avatar))

            val label: String
            var enabled = false
            when (item.relationship) {
                "friends" -> label = "Prijatelj"
                "pending_sent" -> label = "Poslato"
                "pending_received" -> label = "Čeka te"
                else -> {
                    label = "Dodaj"
                    enabled = true
                }
            }
            binding.btnAddFriend.text = label
            binding.btnAddFriend.isEnabled = enabled
            binding.btnAddFriend.setOnClickListener { onAdd(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemFriendSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) = holder.bind(getItem(position))
}
