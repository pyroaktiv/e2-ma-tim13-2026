package rs.tim13.slagalica.mojbroj.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentMojBrojBinding
import kotlin.random.Random

enum class TokenType { NUMBER, OPERATOR, OPEN_BRACKET, CLOSE_BRACKET, EMPTY }
data class ExpressionToken(val text: String, val type: TokenType, val sourceButtonId: Int? = null)

class MojBrojFragment : BaseFragment<FragmentMojBrojBinding>(FragmentMojBrojBinding::inflate) {

    private var stopPhase = 0
    private var trenutnaRunda = 1
    private var rezultatIgrac1 = 0
    private var rezultatIgrac2 = 0

    private var timer: CountDownTimer? = null
    private var autoStopTimer: CountDownTimer? = null

    private val expressionTokens = mutableListOf<ExpressionToken>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.gameHeader.tvPlayer1Score.text = "Igrač 1\n0"
        binding.gameHeader.tvPlayer2Score.text = "Igrač 2\n0"

        setupButtons()
        zapocniRundu()
    }

    private fun zapocniRundu() {
        stopPhase = 0
        expressionTokens.clear()
        osveziPrikazIzraza()

        binding.tvTargetNumber.text = "???"
        binding.glOperations.visibility = View.GONE
        binding.btnSubmit.visibility = View.GONE
        binding.btnStop.visibility = View.VISIBLE

        val numButtons = listOf<Button>(
            binding.btnNum1, binding.btnNum2, binding.btnNum3,
            binding.btnNum4, binding.btnNum5, binding.btnNum6
        )
        numButtons.forEach {
            it.isEnabled = true
            it.text = "?"
        }

        binding.gameHeader.tvGameTimer.text = "60"

        pokreniAutoStopTajmer()
    }

    private fun pokreniAutoStopTajmer() {
        autoStopTimer?.cancel()
        autoStopTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) { }
            override fun onFinish() {
                izvrsiStopAkciju()
            }
        }.start()
    }

    private fun izvrsiStopAkciju() {
        autoStopTimer?.cancel()
        if (stopPhase == 0) {
            stopPhase = 1
            binding.tvTargetNumber.text = Random.nextInt(1, 1000).toString()
            pokreniAutoStopTajmer()
        } else if (stopPhase == 1) {
            stopPhase = 2
            otkrijBrojeve()
            binding.btnStop.visibility = View.GONE
            binding.glOperations.visibility = View.VISIBLE
            binding.btnSubmit.visibility = View.VISIBLE
            zapocniTajmerZaIgru()
        }
    }

    private fun otkrijBrojeve() {
        binding.btnNum1.text = Random.nextInt(1, 10).toString()
        binding.btnNum2.text = Random.nextInt(1, 10).toString()
        binding.btnNum3.text = Random.nextInt(1, 10).toString()
        binding.btnNum4.text = Random.nextInt(1, 10).toString()

        binding.btnNum5.text = listOf(10, 15, 20).random().toString()
        binding.btnNum6.text = listOf(25, 50, 75, 100).random().toString()
    }

    // --- POMOĆNE FUNKCIJE ZA LOGIKU UNOSA ---
    private fun getLastType(): TokenType = expressionTokens.lastOrNull()?.type ?: TokenType.EMPTY
    private fun getUnclosedBrackets(): Int = expressionTokens.count { it.type == TokenType.OPEN_BRACKET } - expressionTokens.count { it.type == TokenType.CLOSE_BRACKET }

    private fun canPressNum(): Boolean {
        val lastType = getLastType()
        return lastType == TokenType.EMPTY || lastType == TokenType.OPERATOR || lastType == TokenType.OPEN_BRACKET
    }

    private fun canPressOp(): Boolean {
        val lastType = getLastType()
        return lastType == TokenType.NUMBER || lastType == TokenType.CLOSE_BRACKET
    }

    private fun canPressOpenBracket(): Boolean {
        val lastType = getLastType()
        return lastType == TokenType.EMPTY || lastType == TokenType.OPERATOR || lastType == TokenType.OPEN_BRACKET
    }

    private fun canPressCloseBracket(): Boolean {
        val lastType = getLastType()
        return (lastType == TokenType.NUMBER || lastType == TokenType.CLOSE_BRACKET) && getUnclosedBrackets() > 0
    }

    private fun isExpressionValid(): Boolean {
        val lastType = getLastType()
        return expressionTokens.isNotEmpty() &&
                getUnclosedBrackets() == 0 &&
                (lastType == TokenType.NUMBER || lastType == TokenType.CLOSE_BRACKET)
    }
    // ----------------------------------------

    private fun setupButtons() {
        binding.btnStop.setOnClickListener {
            izvrsiStopAkciju()
        }

        val numButtons = listOf<Button>(
            binding.btnNum1, binding.btnNum2, binding.btnNum3,
            binding.btnNum4, binding.btnNum5, binding.btnNum6
        )
        numButtons.forEach { button ->
            button.setOnClickListener {
                // Dugme vizuelno ostaje klikabilno celo vreme, ali ako pravilo kaže NE, ništa se ne dešava
                if (canPressNum()) {
                    expressionTokens.add(ExpressionToken(button.text.toString(), TokenType.NUMBER, button.id))
                    button.isEnabled = false // Isključujemo ga da se ne bi kliknuo dvaput isti broj sa stola
                    osveziPrikazIzraza()
                }
            }
        }

        val opButtons = listOf<Button>(
            binding.btnPlus, binding.btnMinus, binding.btnMultiply, binding.btnDivide
        )
        opButtons.forEach { button ->
            button.setOnClickListener {
                if (canPressOp()) {
                    expressionTokens.add(ExpressionToken(button.text.toString(), TokenType.OPERATOR))
                    osveziPrikazIzraza()
                }
            }
        }

        binding.btnOpenBracket.setOnClickListener {
            if (canPressOpenBracket()) {
                expressionTokens.add(ExpressionToken(binding.btnOpenBracket.text.toString(), TokenType.OPEN_BRACKET))
                osveziPrikazIzraza()
            }
        }

        binding.btnCloseBracket.setOnClickListener {
            if (canPressCloseBracket()) {
                expressionTokens.add(ExpressionToken(binding.btnCloseBracket.text.toString(), TokenType.CLOSE_BRACKET))
                osveziPrikazIzraza()
            }
        }

        binding.btnBackspace.setOnClickListener {
            if (expressionTokens.isNotEmpty()) {
                val obrisaniToken = expressionTokens.removeAt(expressionTokens.lastIndex)
                if (obrisaniToken.sourceButtonId != null) {
                    view?.findViewById<Button>(obrisaniToken.sourceButtonId)?.isEnabled = true
                }
                osveziPrikazIzraza()
            }
        }

        // Submit dugme uvek vidljivo, ali proverava da li je izraz dovršen kad se klikne
        binding.btnSubmit.setOnClickListener {
            if (!isExpressionValid()) {
                Toast.makeText(requireContext(), "Matematički izraz nije dovršen!", Toast.LENGTH_SHORT).show()
            } else {
                proveriRešenjeIKrajRunde()
            }
        }
    }

    private fun osveziPrikazIzraza() {
        binding.tvCurrentExpression.text = expressionTokens.joinToString("") { it.text }
    }

    private fun zapocniTajmerZaIgru() {
        timer?.cancel()
        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.gameHeader.tvGameTimer.text = (millisUntilFinished / 1000).toString()
            }
            override fun onFinish() {
                binding.gameHeader.tvGameTimer.text = "0"
                Toast.makeText(requireContext(), "Vreme je isteklo!", Toast.LENGTH_SHORT).show()
                proveriRešenjeIKrajRunde()
            }
        }.start()
    }

    private fun proveriRešenjeIKrajRunde() {
        timer?.cancel()

        if (expressionTokens.isNotEmpty()) {
            if (trenutnaRunda == 1) {
                rezultatIgrac1 += 10
                binding.gameHeader.tvPlayer1Score.text = "Igrač 1\n$rezultatIgrac1"
            } else {
                rezultatIgrac2 += 10
                binding.gameHeader.tvPlayer2Score.text = "Igrač 2\n$rezultatIgrac2"
            }
        }

        if (trenutnaRunda == 1) {
            trenutnaRunda = 2
            Toast.makeText(requireContext(), "Runda 2 - Igra drugi igrač!", Toast.LENGTH_LONG).show()
            Handler(Looper.getMainLooper()).postDelayed({
                zapocniRundu()
            }, 1500)
        } else {
            Toast.makeText(requireContext(), "Kraj igre! P1: $rezultatIgrac1 | P2: $rezultatIgrac2", Toast.LENGTH_LONG).show()
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().popBackStack()
            }, 2000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        autoStopTimer?.cancel()
    }
}