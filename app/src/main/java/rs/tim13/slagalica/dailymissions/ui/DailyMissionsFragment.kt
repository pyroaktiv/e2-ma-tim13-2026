package rs.tim13.slagalica.dailymissions.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import rs.tim13.slagalica.core.network.socket.ServerMessage
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.dailymissions.model.DailyMissionsResponse
import rs.tim13.slagalica.dailymissions.model.MissionDto
import rs.tim13.slagalica.databinding.FragmentDailyMissionsBinding

class DailyMissionsFragment : BaseFragment<FragmentDailyMissionsBinding>(FragmentDailyMissionsBinding::inflate) {

    private val viewModel: DailyMissionsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.missions.observe(viewLifecycleOwner) { data ->
            if (data != null) render(data)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) showError(err)
        }

        viewModel.socketIncoming.observe(viewLifecycleOwner) { msg ->
            if (msg is ServerMessage.MissionProgress || msg is ServerMessage.MissionBonus) {
                viewModel.load()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun render(data: DailyMissionsResponse) {
        binding.tvMissionsDate.text = "Dnevne misije — ${data.date}"

        val cards = listOf(
            Triple(binding.cardMission0, binding.tvMissionTitle0, binding.ivMissionCheck0),
            Triple(binding.cardMission1, binding.tvMissionTitle1, binding.ivMissionCheck1),
            Triple(binding.cardMission2, binding.tvMissionTitle2, binding.ivMissionCheck2),
            Triple(binding.cardMission3, binding.tvMissionTitle3, binding.ivMissionCheck3),
        )

        data.missions.forEachIndexed { i, mission ->
            val (card, tvTitle, ivCheck) = cards.getOrNull(i) ?: return@forEachIndexed
            applyMissionCard(card, tvTitle, ivCheck, mission)
        }

        val completed = data.missions.count { it.completed }
        binding.tvBonusProgress.text = "$completed/4 misija završeno"
        binding.tvBonusUnlocked.visibility = if (data.bonus.allComplete) View.VISIBLE else View.GONE
    }

    private fun applyMissionCard(
        card: com.google.android.material.card.MaterialCardView,
        tvTitle: android.widget.TextView,
        ivCheck: android.widget.ImageView,
        mission: MissionDto
    ) {
        tvTitle.text = mission.title
        ivCheck.visibility = if (mission.completed) View.VISIBLE else View.GONE

        val colorAttr = if (mission.completed)
            com.google.android.material.R.attr.colorSecondaryContainer
        else
            com.google.android.material.R.attr.colorSurface

        val typedValue = android.util.TypedValue()
        card.context.theme.resolveAttribute(colorAttr, typedValue, true)
        card.setCardBackgroundColor(typedValue.data)
    }
}
