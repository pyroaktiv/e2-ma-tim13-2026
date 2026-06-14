package rs.tim13.slagalica.spojnice.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.core.ui.BaseGameFragment
import rs.tim13.slagalica.databinding.FragmentSpojniceBinding
import rs.tim13.slagalica.match.MatchHost

class SpojniceFragment :
    BaseGameFragment<FragmentSpojniceBinding, SpojniceUiState, SpojniceViewModel>(FragmentSpojniceBinding::inflate) {

    private val host get() = requireParentFragment() as MatchHost

    override val viewModel: SpojniceViewModel by viewModels {
        SpojniceViewModelFactory(host.match.spojniceRepository(), host.match.gameConfig)
    }

    override val tvTimer: TextView get() = binding.gameHeader.tvGameTimer

    private lateinit var leftButtons: List<MaterialButton>
    private lateinit var rightButtons: List<MaterialButton>
    private var defaultTint: ColorStateList? = null

    override fun setupUI() {
        leftButtons = listOf(
            binding.btnLeft1, binding.btnLeft2, binding.btnLeft3, binding.btnLeft4, binding.btnLeft5
        )
        rightButtons = listOf(
            binding.btnRight1, binding.btnRight2, binding.btnRight3, binding.btnRight4, binding.btnRight5
        )
        defaultTint = binding.btnLeft1.backgroundTintList

        leftButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.selectLeft(index) }
        }
        rightButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.selectRight(index) }
        }
    }

    override fun renderSpecificState(state: SpojniceUiState) {
        binding.gameHeader.tvPlayer1Score.text = getString(R.string.game_player1_score, state.blueScore)
        binding.gameHeader.tvPlayer2Score.text = getString(R.string.game_player2_score, state.redScore)
        binding.tvRoundLabel.text = getString(R.string.game_round_label, state.round)
        binding.tvActivePlayer.text = getString(R.string.game_active_player, playerNumber(state.activePlayer))
        binding.tvStatusMessage.text = state.statusMessage

        leftButtons.forEachIndexed { index, button ->
            button.text = state.leftItems.getOrNull(index) ?: ""
            val connected = state.connectionsByLeft.getOrNull(index) != null
            val selected = index == state.selectedLeftIndex
            renderConnectableButton(button, connected, selected, state.isMyTurn, state.phase)
        }
        rightButtons.forEachIndexed { index, button ->
            button.text = state.rightItems.getOrNull(index) ?: ""
            val connected = index in state.connectedRightIndices
            renderConnectableButton(button, connected, selected = false, state.isMyTurn, state.phase)
        }
    }

    private fun renderConnectableButton(
        button: MaterialButton,
        connected: Boolean,
        selected: Boolean,
        isMyTurn: Boolean,
        phase: SpojniceGamePhase
    ) {
        when {
            connected -> {
                button.isEnabled = false
                button.backgroundTintList = colorTint(android.R.color.holo_green_light)
            }
            selected -> {
                button.isEnabled = phase == SpojniceGamePhase.PLAYING && isMyTurn
                button.backgroundTintList = colorTint(android.R.color.holo_blue_light)
            }
            else -> {
                button.isEnabled = phase == SpojniceGamePhase.PLAYING && isMyTurn
                button.backgroundTintList = defaultTint
            }
        }
    }

    private fun playerNumber(player: Player): Int = if (player == Player.BLUE) 1 else 2

    private fun colorTint(colorRes: Int): ColorStateList =
        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes))
}
