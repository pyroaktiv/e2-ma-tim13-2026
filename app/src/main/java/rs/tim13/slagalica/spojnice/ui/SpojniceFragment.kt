package rs.tim13.slagalica.spojnice.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentSpojniceBinding
import rs.tim13.slagalica.R

class SpojniceFragment : BaseFragment<FragmentSpojniceBinding>(FragmentSpojniceBinding::inflate) {

    private val likoviRunda1 = listOf("Pop", "Mare", "Gojko Sisa", "Kata", "Policajac")
    private val glumciRunda1 = listOf("Maja Mandžuka", "Nebojša Glogovac", "Boris Milivojević", "Sergej Trifunović", "Nikola Đuričko")

    private val likoviRunda2 = listOf("Kengur", "Braca", "Duje", "Iris", "Živac")
    private val glumciRunda2 = listOf("Marija Karan", "Nebojša Glogovac", "Boris Milivojević", "Nikola Vujović", "Sergej Trifunović")

    private var trenutnaRunda = 1
    private var timer: CountDownTimer? = null

    private var selektovanoLevo: MaterialButton? = null
    private var uparenoPojmova = 0

    private var defaultTextColors: ColorStateList? = null
    private var defaultBackgroundTint: ColorStateList? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        defaultTextColors = binding.btnLeft1.textColors
        defaultBackgroundTint = binding.btnLeft1.backgroundTintList

        setupUI()
    }

    private fun setupUI() {
        popuniRundu()
    }

    private fun popuniRundu() {
        selektovanoLevo = null
        uparenoPojmova = 0

        val likovi = if (trenutnaRunda == 1) likoviRunda1 else likoviRunda2
        val glumci = if (trenutnaRunda == 1) glumciRunda1 else glumciRunda2

        val leftButtons = listOf(binding.btnLeft1, binding.btnLeft2, binding.btnLeft3, binding.btnLeft4, binding.btnLeft5)
        leftButtons.forEachIndexed { index, button ->
            button.text = likovi[index]
            resetButton(button)

            button.setOnClickListener {
                selektovanoLevo?.let { resetButton(it) }
                selektovanoLevo = button
                oznaciSelektovano(button)
            }
        }

        val rightButtons = listOf(binding.btnRight1, binding.btnRight2, binding.btnRight3, binding.btnRight4, binding.btnRight5)
        rightButtons.forEachIndexed { index, button ->
            button.text = glumci[index]
            resetButton(button)

            button.setOnClickListener {
                if (selektovanoLevo != null) {
                    oznaciUpareno(selektovanoLevo!!)
                    oznaciUpareno(button)
                    selektovanoLevo = null
                    uparenoPojmova++

                    if (uparenoPojmova == 5) {
                        sledecaRunda()
                    }
                } else {
                    Toast.makeText(requireContext(), "Prvo izaberite pojam sa leve strane!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        zapocniTajmer()
    }

    private fun zapocniTajmer() {
        timer?.cancel()
        binding.gameHeader.tvGameTimer.text = "30"

        timer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.gameHeader.tvGameTimer.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                binding.gameHeader.tvGameTimer.text = "0"
                sledecaRunda()
            }
        }.start()
    }

    private fun sledecaRunda() {
        if (trenutnaRunda == 1) {
            trenutnaRunda = 2
            Toast.makeText(requireContext(), "Runda 2 (Igrač 2)", Toast.LENGTH_LONG).show()
            popuniRundu()
        } else {
            zavrsiIgru()
        }
    }

    private fun zavrsiIgru() {
        timer?.cancel()
        Toast.makeText(requireContext(), "Kraj igre Spojnice!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun resetButton(button: MaterialButton) {
        button.isEnabled = true
        button.backgroundTintList = defaultBackgroundTint
        button.setTextColor(defaultTextColors)
    }

    private fun oznaciSelektovano(button: MaterialButton) {
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary)
        val onPrimaryColor = getThemeColor(com.google.android.material.R.attr.colorOnPrimary)

        button.backgroundTintList = ColorStateList.valueOf(primaryColor)
        button.setTextColor(onPrimaryColor)
    }

    private fun oznaciUpareno(button: MaterialButton) {
        button.isEnabled = false
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary)
        val onPrimaryColor = getThemeColor(com.google.android.material.R.attr.colorOnPrimary)

        button.backgroundTintList = ColorStateList.valueOf(primaryColor)
        button.setTextColor(onPrimaryColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}