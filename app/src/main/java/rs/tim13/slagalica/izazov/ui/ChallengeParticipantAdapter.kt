package rs.tim13.slagalica.izazov.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.socket.ChallengeParticipantDto
import rs.tim13.slagalica.databinding.ItemChallengeParticipantBinding

class ChallengeParticipantAdapter : RecyclerView.Adapter<ChallengeParticipantAdapter.ViewHolder>() {

    private var items: List<ChallengeParticipantDto> = emptyList()
    private var myUserId: Int? = null
    private var creatorId: Int = -1

    fun submit(items: List<ChallengeParticipantDto>, myUserId: Int?, creatorId: Int) {
        this.items = items
        this.myUserId = myUserId
        this.creatorId = creatorId
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemChallengeParticipantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(participant: ChallengeParticipantDto) {
            val context = binding.root.context
            val suffix = buildString {
                if (participant.userId == creatorId) append(context.getString(R.string.challenge_lobby_host_suffix))
                if (participant.userId == myUserId) append(context.getString(R.string.challenge_lobby_you_suffix))
            }
            binding.tvParticipantName.text = participant.username + suffix
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChallengeParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
