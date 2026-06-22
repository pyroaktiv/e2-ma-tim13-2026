package rs.tim13.slagalica.izazov.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.socket.ChallengeDto
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentChallengeLobbyBinding
import rs.tim13.slagalica.match.MatchMode
import rs.tim13.slagalica.match.ui.MatchHostFragment

class ChallengeLobbyFragment :
    BaseFragment<FragmentChallengeLobbyBinding>(FragmentChallengeLobbyBinding::inflate) {

    private val challengeId: String by lazy { requireArguments().getString(ARG_CHALLENGE_ID).orEmpty() }
    private val viewModel: ChallengeLobbyViewModel by viewModels { ChallengeLobbyViewModelFactory(challengeId) }
    private lateinit var adapter: ChallengeParticipantAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SocketManager.connect(requireContext())

        adapter = ChallengeParticipantAdapter()
        binding.rvParticipants.layoutManager = LinearLayoutManager(requireContext())
        binding.rvParticipants.adapter = adapter

        binding.btnStart.setOnClickListener { viewModel.start() }
        binding.btnLeave.setOnClickListener {
            viewModel.leave()
            findNavController().popBackStack()
        }

        viewModel.fetchProfile(requireContext())
        viewModel.join()

        SocketManager.incoming.observe(viewLifecycleOwner) { message -> viewModel.onServerMessage(message) }
        viewModel.challenge.observe(viewLifecycleOwner) { render(it) }
        viewModel.myUserId.observe(viewLifecycleOwner) { render(viewModel.challenge.value) }
        viewModel.events.observe(viewLifecycleOwner) { handleEvent(it) }
    }

    private fun render(challenge: ChallengeDto?) {
        if (challenge == null) return
        val myUserId = viewModel.myUserId.value
        adapter.submit(challenge.participants, myUserId, challenge.creatorId)

        binding.tvStake.text =
            getString(R.string.challenge_item_stake, challenge.stakeStars, challenge.stakeTokens)

        val isCreator = myUserId != null && myUserId == challenge.creatorId
        val canStart = challenge.participants.size >= 2
        binding.btnStart.visibility = if (isCreator) View.VISIBLE else View.GONE
        binding.btnStart.isEnabled = canStart
        binding.tvWaiting.visibility = if (canStart) View.GONE else View.VISIBLE
    }

    private fun handleEvent(event: ChallengeLobbyEvent?) {
        when (event) {
            is ChallengeLobbyEvent.Started -> {
                viewModel.consumeEvent()
                findNavController().navigate(
                    R.id.action_challengeLobby_to_match,
                    bundleOf(
                        MatchHostFragment.ARG_MODE to MatchMode.CHALLENGE.name,
                        MatchHostFragment.ARG_CHALLENGE_ID to challengeId
                    )
                )
            }
            is ChallengeLobbyEvent.Cancelled -> {
                viewModel.consumeEvent()
                showError(getString(R.string.challenge_lobby_cancelled))
                findNavController().popBackStack()
            }
            is ChallengeLobbyEvent.Error -> {
                viewModel.consumeEvent()
                showError(event.message)
            }
            null -> Unit
        }
    }

    companion object {
        const val ARG_CHALLENGE_ID = "challenge_id"
    }
}
