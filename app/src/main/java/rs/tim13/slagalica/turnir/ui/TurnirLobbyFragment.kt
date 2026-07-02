package rs.tim13.slagalica.turnir.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentTurnirLobbyBinding

class TurnirLobbyFragment : BaseFragment<FragmentTurnirLobbyBinding>(FragmentTurnirLobbyBinding::inflate) {

    private val tvm: TurnirViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvm.reset()
        SocketManager.connect(requireContext())
        tvm.findTournament()

        binding.btnCancel.setOnClickListener { cancel() }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = cancel()
            }
        )

        tvm.state.observe(viewLifecycleOwner) { render(it) }
        tvm.event.observe(viewLifecycleOwner) { handleEvent(it) }
    }

    private fun render(state: TurnirUiState) {
        when (state) {
            is TurnirUiState.Searching -> {
                binding.tvStatus.text = "Tražim igrače..."
                binding.progressBar.visibility = View.VISIBLE
            }
            is TurnirUiState.Queued -> {
                binding.tvStatus.text = "U redu čekanja...\nČekam još igrača."
                binding.progressBar.visibility = View.VISIBLE
            }
            else -> Unit
        }
    }

    private fun handleEvent(event: TurnirEvent?) {
        when (event) {
            is TurnirEvent.NavigateToBracket -> {
                tvm.consumeEvent()
                findNavController().navigate(R.id.action_turnirLobby_to_turnirBracket)
            }
            is TurnirEvent.Cancelled -> {
                tvm.consumeEvent()
                Toast.makeText(requireContext(), "Turnir otkazan.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            null -> Unit
        }
    }

    private fun cancel() {
        tvm.cancelTournament()
        SocketManager.disconnect()
        findNavController().popBackStack()
    }
}
