package rs.tim13.slagalica.korakpokorak.ui

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.viewModels
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseGameFragment
import rs.tim13.slagalica.databinding.FragmentKorakPoKorakBinding
import rs.tim13.slagalica.databinding.ItemKorakPoKorakFieldBinding
import rs.tim13.slagalica.databinding.LayoutGameHeaderBinding
import rs.tim13.slagalica.match.MatchHost

class KorakPoKorakFragment :
    BaseGameFragment<FragmentKorakPoKorakBinding, KorakPoKorakUiState, KorakPoKorakViewModel>(FragmentKorakPoKorakBinding::inflate) {

    private val host get() = requireParentFragment() as MatchHost

    override val viewModel: KorakPoKorakViewModel by viewModels {
        KorakPoKorakViewModelFactory(host.match.korakPoKorakRepository(), host.match.gameConfig)
    }

    override val gameHeader: LayoutGameHeaderBinding get() = binding.gameHeader

    private lateinit var stepViews: List<ItemKorakPoKorakFieldBinding>

    override fun setupUI() {
        stepViews = listOf(
            binding.step1, binding.step2, binding.step3, binding.step4,
            binding.step5, binding.step6, binding.step7
        )

        binding.btnSubmitSolution.setOnClickListener { submitCurrentGuess() }
        binding.etSolution.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitCurrentGuess()
                true
            } else false
        }
    }

    private fun submitCurrentGuess() {
        val guess = binding.etSolution.text?.toString()?.trim().orEmpty()
        if (guess.isEmpty()) return
        viewModel.submitGuess(guess)
        binding.etSolution.text?.clear()
    }

    override fun renderSpecificState(state: KorakPoKorakUiState) {
        binding.gameHeader.tvPlayer1Score.text = getString(R.string.game_player1_score, state.blueScore)
        binding.gameHeader.tvPlayer2Score.text = getString(R.string.game_player2_score, state.redScore)
        binding.tvRoundLabel.text = getString(R.string.game_round_label, state.round)

        binding.tvStatusMessage.text = state.solution?.let { "${state.statusMessage} Rešenje: $it" }
            ?: state.statusMessage

        stepViews.forEachIndexed { index, step -> renderStep(step, index, state) }

        binding.etSolution.isEnabled = state.canGuess
        binding.btnSubmitSolution.isEnabled = state.canGuess
    }

    private fun renderStep(step: ItemKorakPoKorakFieldBinding, index: Int, state: KorakPoKorakUiState) {
        when {
            index >= state.totalSteps -> step.root.visibility = View.GONE
            index < state.revealedClues.size -> {
                step.root.visibility = View.VISIBLE
                step.root.alpha = 1f
                step.tvStepNumber.text = "${index + 1}."
                step.tvStepClue.text = state.revealedClues[index]
            }
            else -> {
                step.root.visibility = View.VISIBLE
                step.root.alpha = 0.4f
                step.tvStepNumber.text = "${index + 1}."
                step.tvStepClue.text = "•••••••"
            }
        }
    }
}
