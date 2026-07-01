package rs.tim13.slagalica.turnir.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.socket.ServerMessage
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.network.socket.TurnirBracketEntryDto
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.core.util.TokenManager
import rs.tim13.slagalica.databinding.FragmentTurnirBracketBinding
import rs.tim13.slagalica.match.MatchMode
import rs.tim13.slagalica.match.ui.MatchHostFragment

class TurnirBracketFragment : BaseFragment<FragmentTurnirBracketBinding>(FragmentTurnirBracketBinding::inflate) {

    private val tvm: TurnirViewModel by activityViewModels()
    private var myUserId: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        myUserId = TokenManager(requireContext()).getUserId()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = onBackAttempt()
            }
        )

        binding.btnAction.setOnClickListener { onActionClick() }

        tvm.state.observe(viewLifecycleOwner) { render(it) }
    }

    private fun render(state: TurnirUiState) {
        val bracket = tvm.foundMessage?.bracket
        when (state) {
            is TurnirUiState.BracketReady -> {
                renderBracketPlayers(bracket)
                binding.tvFinalSlot1.text = "?"
                binding.tvFinalSlot2.text = "?"
                binding.tvStatus.text = "Polufinale počinje! Idi u meč."
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.text = "Idi u meč"
            }
            is TurnirUiState.SemiOver -> {
                renderBracketWithSemiResult(bracket, state.msg)
                binding.tvStatus.text = if (state.msg.won)
                    "Pobedili ste polufinale! Čekam finalnog protivnika..."
                else
                    "Izgubili ste polufinale."
                binding.btnAction.visibility = View.GONE
            }
            is TurnirUiState.FinalReady -> {
                binding.tvStatus.text = "Finale počinje! Protivnik: ${state.msg.opponent.username}"
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.text = "Idi u finale"
            }
            is TurnirUiState.FinalOngoing -> {
                val f = state.msg.finalists
                if (f.size >= 2) {
                    binding.tvFinalSlot1.text = f[0].username
                    binding.tvFinalSlot2.text = f[1].username
                }
                binding.tvStatus.text = "Finale u toku. Čekam rezultat..."
                binding.btnAction.visibility = View.GONE
            }
            is TurnirUiState.TournamentComplete -> {
                val w = state.msg.winner
                val r = state.msg.runner_up
                binding.tvFinalSlot1.text = "★ ${w.username}"
                binding.tvFinalSlot2.text = r.username
                binding.tvStatus.text = buildString {
                    append("Turnir završen!\n")
                    append("Pobednik: ${w.username} (${w.score} bod.)\n")
                    append("Finalista: ${r.username} (${r.score} bod.)")
                }
                binding.btnAction.visibility = View.GONE
            }
            else -> Unit
        }
    }

    private fun renderBracketPlayers(bracket: List<TurnirBracketEntryDto>?) {
        if (bracket == null || bracket.size < 4) return
        binding.tvSemi0P1.text = playerLabel(bracket[0].username, bracket[0].userId)
        binding.tvSemi0P2.text = playerLabel(bracket[1].username, bracket[1].userId)
        binding.tvSemi1P1.text = playerLabel(bracket[2].username, bracket[2].userId)
        binding.tvSemi1P2.text = playerLabel(bracket[3].username, bracket[3].userId)
    }

    private fun renderBracketWithSemiResult(
        bracket: List<TurnirBracketEntryDto>?,
        msg: ServerMessage.TournamentSemiOver
    ) {
        renderBracketPlayers(bracket)
        when (msg.semiIndex) {
            0 -> {
                binding.tvSemi0P1.text = "✓ ${msg.winner.username}"
                binding.tvSemi0P2.text = "✗ ${msg.loser.username}"
                binding.tvFinalSlot1.text = msg.winner.username
            }
            1 -> {
                binding.tvSemi1P1.text = "✓ ${msg.winner.username}"
                binding.tvSemi1P2.text = "✗ ${msg.loser.username}"
                binding.tvFinalSlot2.text = msg.winner.username
            }
        }
    }

    private fun onActionClick() {
        val mode = when (tvm.state.value) {
            is TurnirUiState.BracketReady -> MatchMode.TOURNAMENT_SEMI
            is TurnirUiState.FinalReady -> MatchMode.TOURNAMENT_FINAL
            else -> return
        }
        findNavController().navigate(
            R.id.action_turnirBracket_to_match,
            bundleOf(MatchHostFragment.ARG_MODE to mode.name)
        )
    }

    private fun onBackAttempt() {
        val state = tvm.state.value
        val canLeave = state is TurnirUiState.TournamentComplete ||
                state is TurnirUiState.FinalOngoing ||
                (state is TurnirUiState.SemiOver && !state.msg.won)
        if (canLeave) {
            SocketManager.disconnect()
            tvm.reset()
            findNavController().popBackStack(R.id.homeFragment, false)
        }
        // Block back during BracketReady and FinalReady (user must play their match)
    }

    private fun playerLabel(username: String, userId: Int): String =
        if (userId == myUserId) "$username (ti)" else username
}
