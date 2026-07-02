package rs.tim13.slagalica.friends.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.core.util.ProfileResources
import rs.tim13.slagalica.databinding.ItemFriendBinding
import rs.tim13.slagalica.friends.data.api.dto.FriendDto

/**
 * Lista prijatelja (spec 7.c): avatar sa okvirom lige, korisničko ime, liga, zvezde, mesečni
 * rang i status (online / u igri / offline). Dugme „Pozovi" je aktivno samo ako je prijatelj
 * online i nije u partiji.
 */
class FriendsAdapter(
    private val onInvite: (FriendDto) -> Unit,
    private val onLongPress: (FriendDto) -> Unit
) : ListAdapter<FriendDto, FriendsAdapter.FriendViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FriendDto>() {
            override fun areItemsTheSame(oldItem: FriendDto, newItem: FriendDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FriendDto, newItem: FriendDto) = oldItem == newItem
        }
    }

    inner class FriendViewHolder(private val binding: ItemFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FriendDto) {
            val ctx = binding.root.context
            binding.tvFriendUsername.text = item.username
            binding.ivFriendAvatar.setImageResource(ProfileResources.avatarDrawable(item.avatar))
            binding.ivFriendLeague.setImageResource(ProfileResources.leagueIcon(item.league.icon))
            binding.friendAvatarFrame.backgroundTintList =
                ColorStateList.valueOf(ProfileResources.leagueColor(item.league.icon))

            val rank = item.monthlyRank?.let { "#$it" } ?: "—"
            binding.tvFriendInfo.text =
                "${item.league.name} • ★${item.totalStars} • rang $rank"

            when {
                item.inGame -> {
                    binding.tvFriendStatus.text = "U igri"
                    binding.tvFriendStatus.setTextColor(Color.parseColor("#F9A825"))
                }
                item.isOnline -> {
                    binding.tvFriendStatus.text = "Online"
                    binding.tvFriendStatus.setTextColor(Color.parseColor("#2E7D32"))
                }
                else -> {
                    binding.tvFriendStatus.text = "Offline"
                    binding.tvFriendStatus.setTextColor(Color.parseColor("#9E9E9E"))
                }
            }

            binding.btnInvite.isEnabled = item.isOnline && !item.inGame
            binding.btnInvite.setOnClickListener { onInvite(item) }
            binding.root.setOnLongClickListener { onLongPress(item); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) = holder.bind(getItem(position))
}
