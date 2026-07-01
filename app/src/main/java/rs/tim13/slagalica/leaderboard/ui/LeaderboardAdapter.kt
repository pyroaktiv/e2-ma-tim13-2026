package rs.tim13.slagalica.leaderboard.ui

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.R
import rs.tim13.slagalica.databinding.ItemLeaderboardEntryBinding
import rs.tim13.slagalica.leaderboard.model.LeaderboardEntry

class LeaderboardAdapter(
    private val currentUserId: Int
) : ListAdapter<LeaderboardEntry, LeaderboardAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<LeaderboardEntry>() {
            override fun areItemsTheSame(old: LeaderboardEntry, new: LeaderboardEntry) =
                old.userId == new.userId
            override fun areContentsTheSame(old: LeaderboardEntry, new: LeaderboardEntry) =
                old == new
        }
    }

    inner class ViewHolder(
        private val binding: ItemLeaderboardEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LeaderboardEntry) {
            binding.tvRank.text = "#${entry.rank}"
            binding.tvUsername.text = entry.username
            binding.tvStars.text = "${entry.stars} ★"

            val ctx = binding.root.context
            val iconRes = ctx.resources.getIdentifier(entry.leagueIcon, "drawable", ctx.packageName)
            if (iconRes != 0) binding.ivLeagueIcon.setImageResource(iconRes)
            else binding.ivLeagueIcon.setImageResource(R.drawable.league_bronze)

            val isMe = entry.userId == currentUserId
            if (isMe) {
                binding.root.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.leaderboard_highlight))
            } else {
                val tv = TypedValue()
                ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, tv, true)
                binding.root.setCardBackgroundColor(tv.data)
            }
            binding.tvUsername.setTypeface(
                binding.tvUsername.typeface,
                if (isMe) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLeaderboardEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
