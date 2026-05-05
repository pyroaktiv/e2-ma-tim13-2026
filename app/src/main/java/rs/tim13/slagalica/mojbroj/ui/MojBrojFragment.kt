package rs.tim13.slagalica.mojbroj.ui

import android.os.Bundle
import android.view.View
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentMojBrojBinding

/**
 * Ova klasa trenutno sluzi kao mock da bi se demonstrirao tok interakcije.
 */
class MojBrojFragment : BaseFragment<FragmentMojBrojBinding>(FragmentMojBrojBinding::inflate) {

    private var stopClickCount = 0
    private var currentExpression = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setInitialState()

        binding.btnStop.setOnClickListener {
            stopClickCount++
            if (stopClickCount == 1) {
                binding.tvTargetNumber.text = "425"
            } else if (stopClickCount == 2) {
                showNumbersAndButtons()
            }
        }

        setButtons()
    }

    private fun setInitialState() {
        binding.tvTargetNumber.text = "???"
        binding.tvCurrentExpression.text = ""
        binding.glOperations.visibility = View.GONE
        binding.btnSubmit.visibility = View.GONE
        binding.btnStop.visibility = View.VISIBLE
    }

    private fun showNumbersAndButtons() {
        binding.btnStop.visibility = View.GONE
        binding.glOperations.visibility = View.VISIBLE
        binding.btnSubmit.visibility = View.VISIBLE

        binding.btnNum1.text = "1"
        binding.btnNum2.text = "3"
        binding.btnNum3.text = "7"
        binding.btnNum4.text = "9"
        binding.btnNum5.text = "15"
        binding.btnNum6.text = "75"
    }

    private fun setButtons() {
        val inputButtons = listOf(
            binding.btnNum1, binding.btnNum2, binding.btnNum3,
            binding.btnNum4, binding.btnNum5, binding.btnNum6,
            binding.btnPlus, binding.btnMinus, binding.btnMultiply,
            binding.btnDivide, binding.btnOpenBracket, binding.btnCloseBracket
        )

        inputButtons.forEach { button ->
            button.setOnClickListener {
                currentExpression += button.text.toString()
                binding.tvCurrentExpression.text = currentExpression
                if (button.id in listOf(R.id.btnNum1, R.id.btnNum2, R.id.btnNum3, R.id.btnNum4, R.id.btnNum5, R.id.btnNum6)) {
                    button.isEnabled = false
                }
            }
        }

        binding.btnBackspace.setOnClickListener {
            if (currentExpression.isNotEmpty()) {
                currentExpression = currentExpression.dropLast(1)
                binding.tvCurrentExpression.text = currentExpression
            }
        }

        binding.btnSubmit.setOnClickListener {
            if (currentExpression.isEmpty()) {
                showError("Morate uneti izraz!")
            } else {
                showError("Izraz predat: $currentExpression")
            }
        }
    }
}