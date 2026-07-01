package rs.tim13.slagalica.friends.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.core.util.ProfileResources
import rs.tim13.slagalica.databinding.ItemFriendRequestBinding
import rs.tim13.slagalica.friends.data.api.dto.FriendRequestDto

/** Lista pristiglih zahteva za prijateljstvo (spec 7) sa prihvati/odbij dugmadima. */
class FriendRequestsAdapter(
    private val onAccept: (FriendRequestDto) -> Unit,
    private val onDecline: (FriendRequestDto) -> Unit
) : ListAdapter<FriendRequestDto, FriendRequestsAdapter.RequestViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FriendRequestDto>() {
            override fun areItemsTheSame(oldItem: FriendRequestDto, newItem: FriendRequestDto) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FriendRequestDto, newItem: FriendRequestDto) =
                oldItem == newItem
        }
    }

    inner class RequestViewHolder(private val binding: ItemFriendRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FriendRequestDto) {
            binding.tvRequestUsername.text = item.from.username
            binding.ivRequestAvatar.setImageResource(ProfileResources.avatarDrawable(item.from.avatar))
            binding.btnAccept.setOnClickListener { onAccept(item) }
            binding.btnDecline.setOnClickListener { onDecline(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemFriendRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) = holder.bind(getItem(position))
}
