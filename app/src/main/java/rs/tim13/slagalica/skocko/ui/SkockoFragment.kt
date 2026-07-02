package rs.tim13.slagalica.skocko.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseGameFragment
import rs.tim13.slagalica.databinding.FragmentSkockoBinding
import rs.tim13.slagalica.databinding.ItemSkockoCellBinding
import rs.tim13.slagalica.databinding.LayoutGameHeaderBinding
import rs.tim13.slagalica.match.MatchHost
import rs.tim13.slagalica.skocko.model.SkockoHint
import rs.tim13.slagalica.skocko.model.SkockoSymbol

class SkockoFragment : BaseGameFragment<FragmentSkockoBinding, SkockoUiState, SkockoViewModel>(FragmentSkockoBinding::inflate) {

    private val host get() = requireParentFragment() as MatchHost

    override val viewModel: SkockoViewModel by viewModels {
        SkockoViewModelFactory(host.match.skockoRepository(), host.match.gameConfig)
    }

    // Ugovor iz BaseGameFragment-a
    override val gameHeader: LayoutGameHeaderBinding get() = binding.gameHeader

    // gridCells[row][col], rows 0-5 = main, 6 = bonus, 7 = solution
    private lateinit var gridCells: Array<Array<ItemSkockoCellBinding>>
    private val rowLayouts = mutableListOf<LinearLayout>()

    private val symbolDrawable = mapOf(
        SkockoSymbol.SKOCKO  to R.drawable.ic_skocko,
        SkockoSymbol.KVADRAT to R.drawable.ic_kvadrat,
        SkockoSymbol.KRUG    to R.drawable.ic_krug,
        SkockoSymbol.SRCE    to R.drawable.ic_srce,
        SkockoSymbol.TROUGAO to R.drawable.ic_trougao,
        SkockoSymbol.ZVEZDA  to R.drawable.ic_zvezda,
    )

    override fun setupUI() {
        setupGrid()
        setupKeyboard()
    }

    // region Grid setup

    private fun setupGrid() {
        gridCells = Array(8) { Array(4) { inflateCell() } }
        rowLayouts.clear()

        for (row in 0..5) {
            rowLayouts.add(makeRow(row).also { binding.llGrid.addView(it) })
        }
        binding.llGrid.addView(makeDivider())
        rowLayouts.add(makeRow(6, getString(R.string.skocko_bonus_row)).also { binding.llGrid.addView(it) })
        rowLayouts.add(makeRow(7, getString(R.string.skocko_solution_row)).also { binding.llGrid.addView(it) })

        // After first layout: cap row height to cell width so cells are at most 1:1
        binding.llGrid.post {
            val maxCellH = binding.llGrid.width / 4
            rowLayouts.forEach { row ->
                if (row.height > maxCellH) {
                    row.layoutParams = (row.layoutParams as LinearLayout.LayoutParams).also { lp ->
                        lp.height = maxCellH
                        lp.weight = 0f
                    }
                }
            }
            binding.llGrid.requestLayout()
        }
    }

    private fun makeRow(row: Int, label: String? = null): LinearLayout {
        val margin = (2 * resources.displayMetrics.density).toInt()
        val rowLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ).also { it.bottomMargin = margin }
        }

        if (label != null) {
            rowLayout.addView(TextView(requireContext()).apply {
                text = label
                textSize = 16f
                width = (72 * resources.displayMetrics.density).toInt()
                gravity = android.view.Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            })
        }

        repeat(4) { col -> rowLayout.addView(gridCells[row][col].root) }
        return rowLayout
    }

    private fun makeDivider(): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (2 * resources.displayMetrics.density).toInt()
        ).also { it.topMargin = (2 * resources.displayMetrics.density).toInt() }
        setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
    }

    private fun inflateCell(): ItemSkockoCellBinding =
        ItemSkockoCellBinding.inflate(layoutInflater, binding.llGrid, false)

    // endregion

    // region Keyboard

    private fun setupKeyboard() {
        binding.btnSkocko.setOnClickListener  { viewModel.addSymbol(SkockoSymbol.SKOCKO) }
        binding.btnKvadrat.setOnClickListener { viewModel.addSymbol(SkockoSymbol.KVADRAT) }
        binding.btnKrug.setOnClickListener    { viewModel.addSymbol(SkockoSymbol.KRUG) }
        binding.btnSrce.setOnClickListener    { viewModel.addSymbol(SkockoSymbol.SRCE) }
        binding.btnTrougao.setOnClickListener { viewModel.addSymbol(SkockoSymbol.TROUGAO) }
        binding.btnZvezda.setOnClickListener  { viewModel.addSymbol(SkockoSymbol.ZVEZDA) }
        binding.btnErase.setOnClickListener   { viewModel.eraseSymbol() }
        binding.btnSubmit.setOnClickListener  { viewModel.submitGuess() }
    }

    // endregion

    // region State rendering

    override fun renderSpecificState(state: SkockoUiState) {
        updateHeader(state)
        updateGrid(state)
        updateKeyboard(state)
    }

    private fun updateHeader(state: SkockoUiState) {
        binding.tvRoundLabel.text = getString(R.string.skocko_round_label, state.round)
        binding.tvStatusMessage.text = state.statusMessage
        binding.gameHeader.tvPlayer1Score.text = getString(R.string.game_player1_score, state.blueScore)
        binding.gameHeader.tvPlayer2Score.text = getString(R.string.game_player2_score, state.redScore)
    }

    private fun updateGrid(state: SkockoUiState) {
        val activeMainRow = state.mainGuesses.size  // 0-6; row where current input goes

        // Rows 0-5: main guess rows
        for (row in 0..5) {
            when {
                row < state.mainGuesses.size -> {
                    // Evaluated guess
                    val guess = state.mainGuesses[row]
                    for (col in 0..3) {
                        setCellSymbol(row, col, guess.symbols[col])
                        setCellHintColor(row, col, guess.hints[col])
                    }
                }
                row == activeMainRow && state.phase == SkockoGamePhase.MAIN_TURN -> {
                    // Active input row — show partial currentInput
                    for (col in 0..3) {
                        val sym = state.currentInput.getOrNull(col)
                        setCellSymbol(row, col, sym)
                        setCellActiveBackground(row, col)
                    }
                }
                else -> {
                    // Empty locked row
                    for (col in 0..3) {
                        setCellSymbol(row, col, null)
                        setCellDefaultBackground(row, col)
                    }
                }
            }
        }

        // Row 6: bonus row
        when {
            state.bonusGuess != null -> {
                val guess = state.bonusGuess
                for (col in 0..3) {
                    setCellSymbol(6, col, guess.symbols[col])
                    setCellHintColor(6, col, guess.hints[col])
                }
            }
            state.phase == SkockoGamePhase.BONUS_TURN -> {
                for (col in 0..3) {
                    val sym = state.currentInput.getOrNull(col)
                    setCellSymbol(6, col, sym)
                    setCellActiveBackground(6, col)
                }
            }
            else -> {
                for (col in 0..3) {
                    setCellSymbol(6, col, null)
                    setCellDefaultBackground(6, col)
                }
            }
        }

        // Row 7: solution row
        if (state.secret != null) {
            for (col in 0..3) {
                setCellSymbol(7, col, state.secret[col])
                gridCells[7][col].root.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.skocko_solution)
                )
            }
        } else {
            for (col in 0..3) {
                setCellSymbol(7, col, null)
                setCellDefaultBackground(7, col)
            }
        }
    }

    private fun updateKeyboard(state: SkockoUiState) {
        val enabled = state.isInputEnabled
        listOf(
            binding.btnSkocko, binding.btnKvadrat, binding.btnKrug,
            binding.btnSrce, binding.btnTrougao, binding.btnZvezda, binding.btnErase
        ).forEach { it.isEnabled = enabled }
        binding.btnSubmit.isEnabled = enabled && state.currentInput.size == 4

        val roundOver = state.phase == SkockoGamePhase.ROUND_OVER || state.phase == SkockoGamePhase.GAME_OVER
        binding.keyboardBtnRow.visibility = if (!roundOver) View.VISIBLE else View.GONE
    }

    // endregion

    // region Cell helpers

    private fun setCellSymbol(row: Int, col: Int, symbol: SkockoSymbol?) {
        val iv = gridCells[row][col].ivSymbol
        val drawableRes = symbol?.let { symbolDrawable[it] }
        if (drawableRes != null) {
            iv.setImageResource(drawableRes)
            iv.visibility = View.VISIBLE
        } else {
            iv.setImageDrawable(null)
            iv.visibility = View.INVISIBLE
        }
    }

    private fun setCellHintColor(row: Int, col: Int, hint: SkockoHint) {
        val colorRes = when (hint) {
            SkockoHint.CORRECT -> R.color.skocko_correct
            SkockoHint.PRESENT -> R.color.skocko_present
            SkockoHint.ABSENT  -> android.R.color.transparent
        }
        gridCells[row][col].root.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), colorRes)
        )
    }

    private fun setCellActiveBackground(row: Int, col: Int) {
        gridCells[row][col].root.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.background_light)
        )
    }

    private fun setCellDefaultBackground(row: Int, col: Int) {
        gridCells[row][col].root.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        )
    }

    // endregion
}
