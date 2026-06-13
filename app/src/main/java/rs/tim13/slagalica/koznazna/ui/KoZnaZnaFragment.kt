package rs.tim13.slagalica.koznazna.ui

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.model.GameConfig
import rs.tim13.slagalica.core.ui.BaseGameFragment
import rs.tim13.slagalica.databinding.FragmentKoZnaZnaBinding
import rs.tim13.slagalica.koznazna.data.MockKoZnaZnaGameRepository

class KoZnaZnaFragment :
    BaseGameFragment<FragmentKoZnaZnaBinding, KoZnaZnaUiState, KoZnaZnaViewModel>(FragmentKoZnaZnaBinding::inflate) {

    override val viewModel: KoZnaZnaViewModel by viewModels {
        KoZnaZnaViewModelFactory(
            repository = MockKoZnaZnaGameRepository(),
            config = GameConfig.fromBundle(arguments)
        )
    }

    override val tvTimer: TextView get() = binding.gameHeader.tvGameTimer

    private lateinit var answerButtons: List<MaterialButton>
    private var defaultTint: ColorStateList? = null

    override fun setupUI() {
        answerButtons = listOf(binding.btnAnswerA, binding.btnAnswerB, binding.btnAnswerC, binding.btnAnswerD)
        defaultTint = binding.btnAnswerA.backgroundTintList
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.submitAnswer(index) }
        }
    }

    override fun renderSpecificState(state: KoZnaZnaUiState) {
        binding.gameHeader.tvPlayer1Score.text = getString(R.string.game_player1_score, state.blueScore)
        binding.gameHeader.tvPlayer2Score.text = getString(R.string.game_player2_score, state.redScore)

        binding.tvQuestionProgress.text =
            getString(R.string.game_question_progress, state.questionNumber, state.questionCount)
        binding.tvQuestion.text = "${state.questionNumber}. ${state.questionText}"
        binding.tvStatusMessage.text = state.statusMessage

        answerButtons.forEachIndexed { index, button ->
            button.text = state.options.getOrNull(index) ?: ""
            renderAnswerButton(button, index, state)
        }
    }

    private fun renderAnswerButton(button: MaterialButton, index: Int, state: KoZnaZnaUiState) {
        when {
            // Faza otkrivanja: tačan zeleno, moj pogrešan crveno.
            state.correctIndex != null -> {
                button.isEnabled = false
                button.backgroundTintList = when (index) {
                    state.correctIndex -> colorTint(android.R.color.holo_green_light)
                    state.myAnswerIndex -> colorTint(android.R.color.holo_red_light)
                    else -> defaultTint
                }
            }
            // Već sam odgovorio, čekam protivnika/tajmer.
            state.myAnswerIndex != null -> {
                button.isEnabled = false
                button.backgroundTintList =
                    if (index == state.myAnswerIndex) colorTint(android.R.color.holo_blue_light) else defaultTint
            }
            // Aktivno pitanje.
            else -> {
                button.isEnabled = true
                button.backgroundTintList = defaultTint
            }
        }
    }

    private fun colorTint(colorRes: Int): ColorStateList =
        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes))
}
