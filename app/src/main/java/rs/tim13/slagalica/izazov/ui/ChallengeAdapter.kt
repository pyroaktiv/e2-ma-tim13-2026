package rs.tim13.slagalica.izazov.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.socket.ChallengeDto
import rs.tim13.slagalica.databinding.ItemChallengeBinding

class ChallengeAdapter(
    private val onClick: (ChallengeDto) -> Unit
) : ListAdapter<ChallengeDto, ChallengeAdapter.ChallengeViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChallengeDto>() {
            override fun areItemsTheSame(oldItem: ChallengeDto, newItem: ChallengeDto) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ChallengeDto, newItem: ChallengeDto) = oldItem == newItem
        }
    }

    inner class ChallengeViewHolder(private val binding: ItemChallengeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChallengeDto) {
            val context = binding.root.context
            binding.tvChallengeCreator.text = context.getString(R.string.challenge_item_creator, item.creatorUsername)
            binding.tvChallengeStake.text =
                context.getString(R.string.challenge_item_stake, item.stakeStars, item.stakeTokens)
            binding.tvChallengeParticipants.text =
                context.getString(R.string.challenge_item_participants, item.participants.size, 4)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val binding = ItemChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChallengeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
