package rs.tim13.slagalica.asocijacije.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import rs.tim13.slagalica.R
import rs.tim13.slagalica.asocijacije.model.AssociationsColumn
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentAsocijacijeBinding
import rs.tim13.slagalica.databinding.ItemAsocijacijeCellBinding

class AsocijacijeFragment :
    BaseFragment<FragmentAsocijacijeBinding>(FragmentAsocijacijeBinding::inflate) {

    private val viewModel: AssociationsViewModel by viewModels()

    // [columnIndex][fieldIndex] → cell binding; solutionBindings[columnIndex]
    private lateinit var fieldBindings: Array<Array<ItemAsocijacijeCellBinding>>
    private lateinit var solutionBindings: Array<ItemAsocijacijeCellBinding>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGrid()
        setupInputButtons()
        viewModel.uiState.observe(viewLifecycleOwner, ::renderState)
    }

    // region Grid setup

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

            // Column label header
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

    // endregion

    // region Input buttons

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
        binding.btnNextRound.setOnClickListener { viewModel.advanceToNextRound() }

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

    // endregion

    // region State rendering

    private fun renderState(state: AssociationsUiState) {
        updateHeader(state)
        updateGrid(state)
        updatePhaseUi(state)
    }

    private fun updateHeader(state: AssociationsUiState) {
        binding.tvRoundLabel.text = "Runda ${state.round} / 2"
        binding.tvActivePlayer.text = "Na potezu: Igrač ${state.activePlayer.color}"
        binding.gameHeader.tvPlayer1Score.text = "Igrač 1\n${state.blueScore}"
        binding.gameHeader.tvPlayer2Score.text = "Igrač 2\n${state.redScore}"
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
                    b.root.isClickable = !column.isSolved && state.isNextMoveRevealing
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
        val roundOver = state.phase == GamePhase.ROUND_OVER || state.phase == GamePhase.GAME_OVER
        binding.btnNextRound.visibility =  if ( roundOver) View.VISIBLE else View.GONE
        binding.btnGuessFinal.visibility = if (!roundOver) View.VISIBLE else View.GONE
        binding.btnNextRound.text = if (state.phase == GamePhase.GAME_OVER) "Kraj igre" else "Sledeća runda"

        listOf(binding.btnGuessA, binding.btnGuessB, binding.btnGuessC, binding.btnGuessD, binding.btnGuessFinal)
            .forEach { it.isEnabled = !state.isNextMoveRevealing }

        if (state.phase == GamePhase.GAME_OVER) {
            val winner = when {
                state.blueScore > state.redScore -> "Plavi igrač je pobedio!"
                state.redScore > state.blueScore -> "Crveni igrač je pobedio!"
                else -> "Nerešeno!"
            }
            binding.tvStatusMessage.text = winner
            binding.btnGuessFinal.isEnabled = false
            listOf(binding.btnGuessA, binding.btnGuessB, binding.btnGuessC, binding.btnGuessD)
                .forEach { it.isEnabled = false }
        }
    }

    // endregion
}