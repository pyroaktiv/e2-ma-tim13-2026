package rs.tim13.slagalica.asocijacije.ui

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseGameFragment
import rs.tim13.slagalica.databinding.FragmentAsocijacijeBinding
import rs.tim13.slagalica.databinding.ItemAsocijacijeCellBinding
import rs.tim13.slagalica.match.MatchHost

class AssociationsFragment :
    BaseGameFragment<FragmentAsocijacijeBinding, AssociationsUiState, AssociationsViewModel>(FragmentAsocijacijeBinding::inflate) {

    private val host get() = requireParentFragment() as MatchHost

    override val viewModel: AssociationsViewModel by viewModels {
        AssociationsViewModelFactory(host.match.asocijacijeRepository(), host.match.gameConfig)
    }

    override val tvTimer: TextView get() = binding.gameHeader.tvGameTimer

    private lateinit var fieldBindings: Array<Array<ItemAsocijacijeCellBinding>>
    private lateinit var solutionBindings: Array<ItemAsocijacijeCellBinding>

    override fun setupUI() {
        setupGrid()
        setupInputButtons()
    }

    override fun renderSpecificState(state: AssociationsUiState) {
        updateCustomHeaderInfo(state)
        updateGrid(state)
        updatePhaseUi(state)
    }

    private fun setupGrid() {
        val numCols = 4
        fieldBindings = Array(numCols) { col ->
            Array(4) { row -> createFieldCell(col, row) }
        }
        solutionBindings = Array(numCols) { col -> createSolutionCell(col) }

        repeat(numCols) { col ->
            val colLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
                )
            }

            colLayout.addView(TextView(requireContext()).apply {
                text = listOf("A", "B", "C", "D")[col]
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                setBackgroundColor(requireContext().getColor(com.google.android.material.R.color.material_dynamic_primary40))
                setPadding(0, 4, 0, 4)
            })

            repeat(4) { row -> colLayout.addView(fieldBindings[col][row].root) }
            colLayout.addView(solutionBindings[col].root)

            binding.llColumnsContainer.addView(colLayout)
        }
    }

    private fun createFieldCell(col: Int, row: Int): ItemAsocijacijeCellBinding {
        val b = ItemAsocijacijeCellBinding.inflate(layoutInflater, binding.llColumnsContainer, false)
        b.tvCellContent.text = "?"
        b.root.setOnClickListener { viewModel.revealField(col, row) }
        return b
    }

    private fun createSolutionCell(col: Int): ItemAsocijacijeCellBinding {
        val b = ItemAsocijacijeCellBinding.inflate(layoutInflater, binding.llColumnsContainer, false)
        b.tvCellContent.text = "?"
        b.root.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
        )
        b.root.isClickable = false
        return b
    }

    private fun setupInputButtons() {
        val colButtons = listOf(
            binding.btnGuessA to 0,
            binding.btnGuessB to 1,
            binding.btnGuessC to 2,
            binding.btnGuessD to 3
        )
        colButtons.forEach { (btn, index) ->
            btn.setOnClickListener { submitColumnGuess(index) }
        }
        binding.btnGuessFinal.setOnClickListener { submitFinalGuess() }

        binding.etGuess.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitFinalGuess()
                true
            } else false
        }
    }

    private fun submitColumnGuess(columnIndex: Int) {
        val guess = binding.etGuess.text?.toString()?.trim() ?: return
        if (guess.isEmpty()) { showError("Unesite odgovor"); return }
        val correct = viewModel.guessColumn(columnIndex, guess)
        val colLabel = listOf("A", "B", "C", "D")[columnIndex]
        binding.tvStatusMessage.text = if (correct)
            "✓ Tačno! Kolona $colLabel"
        else
            "✗ Netačno. Sledeći igrač na potezu."
        binding.etGuess.text?.clear()
    }

    private fun submitFinalGuess() {
        val guess = binding.etGuess.text?.toString()?.trim() ?: return
        if (guess.isEmpty()) { showError("Unesite odgovor"); return }
        val correct = viewModel.guessFinal(guess)
        binding.tvStatusMessage.text = if (correct)
            "✓ Tačno konačno rešenje!"
        else
            "✗ Netačno. Sledeći igrač na potezu."
        binding.etGuess.text?.clear()
    }

    private fun updateCustomHeaderInfo(state: AssociationsUiState) {
        binding.tvRoundLabel.visibility = View.VISIBLE
        binding.tvRoundLabel.text = getString(R.string.asocijacije_round_label, state.round)
        binding.tvActivePlayer.text = getString(R.string.asocijacije_active_player, state.activePlayer.name)
    }

    private fun updateGrid(state: AssociationsUiState) {
        state.columns.forEachIndexed { col, column ->
            column.fields.forEachIndexed { row, field ->
                val b = fieldBindings[col][row]
                if (field.isRevealed) {
                    b.tvCellContent.text = field.text
                    b.root.isClickable = false
                    b.root.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), android.R.color.background_light)
                    )
                } else {
                    b.tvCellContent.text = "?"
                    b.root.isClickable = !column.isSolved && state.isNextMoveRevealing && state.isMyTurn
                }
            }

            val sb = solutionBindings[col]
            if (column.isSolutionRevealed) {
                sb.tvCellContent.text = column.solution
                sb.root.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                )
            } else {
                sb.tvCellContent.text = "?"
            }
        }

        binding.tvFinalSolution.text = if (state.isFinalSolved) state.finalSolution else "Krajnje rešenje"
    }

    private fun updatePhaseUi(state: AssociationsUiState) {
        val roundOver = state.phase == AssociationsGamePhase.ROUND_OVER || state.phase == AssociationsGamePhase.GAME_OVER
        binding.btnGuessFinal.visibility = if (!roundOver) View.VISIBLE else View.GONE

        val canGuess = !state.isNextMoveRevealing && state.isMyTurn
        listOf(binding.btnGuessA, binding.btnGuessB, binding.btnGuessC, binding.btnGuessD, binding.btnGuessFinal)
            .forEach { it.isEnabled = canGuess }
        binding.etGuess.isEnabled = canGuess

        if (state.phase == AssociationsGamePhase.GAME_OVER) {
            binding.tvStatusMessage.text = "Kraj Asocijacija!"
            binding.btnGuessFinal.isEnabled = false
            listOf(binding.btnGuessA, binding.btnGuessB, binding.btnGuessC, binding.btnGuessD)
                .forEach { it.isEnabled = false }
        }
    }
}