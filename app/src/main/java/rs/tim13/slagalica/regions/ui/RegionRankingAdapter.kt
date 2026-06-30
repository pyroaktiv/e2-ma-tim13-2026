package rs.tim13.slagalica.regions.ui

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.util.ProfileResources
import rs.tim13.slagalica.databinding.ItemRegionBinding
import rs.tim13.slagalica.regions.data.api.dto.RegionRankEntryDto

/** Mesečna rang lista regiona (spec 5.b). Region igrača je posebno označen. */
class RegionRankingAdapter(
    private val onClick: (RegionRankEntryDto) -> Unit
) : ListAdapter<RegionRankEntryDto, RegionRankingAdapter.RegionViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RegionRankEntryDto>() {
            override fun areItemsTheSame(oldItem: RegionRankEntryDto, newItem: RegionRankEntryDto) =
                oldItem.region == newItem.region
            override fun areContentsTheSame(oldItem: RegionRankEntryDto, newItem: RegionRankEntryDto) =
                oldItem == newItem
        }
    }

    inner class RegionViewHolder(private val binding: ItemRegionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RegionRankEntryDto) {
            val ctx = binding.root.context
            binding.tvRegionRank.text = "${item.rank}."
            binding.ivRegionIcon.setImageResource(R.drawable.ic_region_pin)
            binding.ivRegionIcon.setColorFilter(ProfileResources.regionColor(item.region))
            binding.tvRegionStars.text = "★ ${item.stars}"

            // Spec 5.b: posebno označiti region kom igrač pripada.
            if (item.isMine) {
                binding.tvRegionName.text = ctx.getString(R.string.regions_mine_suffix, item.region)
                binding.tvRegionName.setTypeface(null, Typeface.BOLD)
            } else {
                binding.tvRegionName.text = item.region
                binding.tvRegionName.setTypeface(null, Typeface.NORMAL)
            }

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionViewHolder {
        val binding = ItemRegionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RegionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RegionViewHolder, position: Int) = holder.bind(getItem(position))
}
