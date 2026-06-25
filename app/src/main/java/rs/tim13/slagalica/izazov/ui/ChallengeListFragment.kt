package rs.tim13.slagalica.izazov.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.DialogCreateChallengeBinding
import rs.tim13.slagalica.databinding.FragmentChallengeListBinding

class ChallengeListFragment :
    BaseFragment<FragmentChallengeListBinding>(FragmentChallengeListBinding::inflate) {

    private val viewModel: ChallengeListViewModel by viewModels()
    private lateinit var adapter: ChallengeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SocketManager.connect(requireContext())

        adapter = ChallengeAdapter { challenge -> openLobby(challenge.id) }
        binding.rvChallenges.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChallenges.adapter = adapter

        binding.btnRefresh.setOnClickListener { viewModel.refresh(requireContext()) }
        binding.fabCreateChallenge.setOnClickListener { showCreateDialog() }

        viewModel.challenges.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        SocketManager.incoming.observe(viewLifecycleOwner) { message -> viewModel.onServerMessage(message) }

        viewModel.events.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ChallengeListEvent.Created -> {
                    viewModel.consumeEvent()
                    openLobby(event.challengeId)
                }
                is ChallengeListEvent.Error -> {
                    viewModel.consumeEvent()
                    showError(event.message)
                }
                null -> Unit
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh(requireContext())
    }

    private fun showCreateDialog() {
        val dialogBinding = DialogCreateChallengeBinding.inflate(layoutInflater)
        dialogBinding.tvStarsLabel.text = getString(R.string.challenge_create_stars_label, 0)
        dialogBinding.tvTokensLabel.text = getString(R.string.challenge_create_tokens_label, 0)
        dialogBinding.sliderStars.addOnChangeListener { _, value, _ ->
            dialogBinding.tvStarsLabel.text = getString(R.string.challenge_create_stars_label, value.toInt())
        }
        dialogBinding.sliderTokens.addOnChangeListener { _, value, _ ->
            dialogBinding.tvTokensLabel.text = getString(R.string.challenge_create_tokens_label, value.toInt())
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.challenge_create_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.challenge_create_confirm) { _, _ ->
                viewModel.createChallenge(
                    stars = dialogBinding.sliderStars.value.toInt(),
                    tokens = dialogBinding.sliderTokens.value.toInt()
                )
            }
            .setNegativeButton(R.string.challenge_cancel, null)
            .show()
    }

    private fun openLobby(challengeId: String) {
        findNavController().navigate(
            R.id.action_challengeList_to_challengeLobby,
            bundleOf(ChallengeLobbyFragment.ARG_CHALLENGE_ID to challengeId)
        )
    }
}
