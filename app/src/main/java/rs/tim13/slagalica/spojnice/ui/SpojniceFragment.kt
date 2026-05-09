package rs.tim13.slagalica.spojnice.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentSpojniceBinding

class SpojniceFragment : BaseFragment<FragmentSpojniceBinding>(FragmentSpojniceBinding::inflate) {

    private val likoviRunda1 = listOf("Pop", "Mare", "Gojko Sisa", "Kata", "Policajac")
    private val glumciRunda1 = listOf("Sergej Trifunović", "Nebojša Glogovac", "Nikola Đuričko", "Maja Mandžuka", "Boris Milivojević")
    private val resenjaRunda1 = mapOf(0 to 0, 1 to 4, 2 to 2, 3 to 3, 4 to 1)

    private val likoviRunda2 = listOf("Kengur", "Braca", "Šomi", "Iris", "Živac")
    private val glumciRunda2 = listOf("Marija Karan", "Nebojša Glogovac", "Boris Milivojević", "Nikola Đuričko", "Sergej Trifunović")
    private val resenjaRunda2 = mapOf(0 to 3, 1 to 4, 2 to 2, 3 to 0, 4 to 1)

    private var trenutnaRunda = 1
    private var leadPlayer = 1
    private var activePlayer = 1
    private var timer: CountDownTimer? = null

    private var rezultatIgrac1 = 0
    private var rezultatIgrac2 = 0

    private var selektovanoLevo: MaterialButton? = null
    private var indeksSelektovanogLevog: Int? = null

    private var pokusajiVodeceg = 0
    private var ukupnoSredjeno = 0
    private var pokusajiPopravnog = 0
    private var dozvoljenoPopravki = 0
    private val spojenoLevo = BooleanArray(5)

    private var defaultTextColors: ColorStateList? = null
    private var defaultBackgroundTint: ColorStateList? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        defaultTextColors = binding.btnLeft1.textColors
        defaultBackgroundTint = binding.btnLeft1.backgroundTintList

        binding.gameHeader.tvPlayer1Score.text = "Igrač 1\n0"
        binding.gameHeader.tvPlayer2Score.text = "Igrač 2\n0"

        setupUI()
    }

    private fun setupUI() {
        popuniRundu()
    }

    private fun popuniRundu() {
        selektovanoLevo = null
        indeksSelektovanogLevog = null
        pokusajiVodeceg = 0
        ukupnoSredjeno = 0
        pokusajiPopravnog = 0
        dozvoljenoPopravki = 0
        spojenoLevo.fill(false)

        activePlayer = if (trenutnaRunda == 1) 1 else 2
        leadPlayer = activePlayer

        val likovi = if (trenutnaRunda == 1) likoviRunda1 else likoviRunda2
        val glumci = if (trenutnaRunda == 1) glumciRunda1 else glumciRunda2

        osveziListuDugmica(likovi, glumci)
        zapocniTajmer()
    }

    private fun osveziListuDugmica(likovi: List<String>, glumci: List<String>) {
        val leftButtons = listOf(binding.btnLeft1, binding.btnLeft2, binding.btnLeft3, binding.btnLeft4, binding.btnLeft5)
        leftButtons.forEachIndexed { index, button ->
            button.text = likovi[index]
            resetButton(button)
            button.setOnClickListener {
                selektovanoLevo?.let { resetButton(it) }
                selektovanoLevo = button
                indeksSelektovanogLevog = index
                oznaciSelektovano(button)
            }
        }

        val rightButtons = listOf(binding.btnRight1, binding.btnRight2, binding.btnRight3, binding.btnRight4, binding.btnRight5)
        rightButtons.forEachIndexed { index, button ->
            button.text = glumci[index]
            resetButton(button)
            button.setOnClickListener { proveriSpojnicu(index, button) }
        }
    }

    private fun proveriSpojnicu(desniIndex: Int, desnoDugme: MaterialButton) {
        if (selektovanoLevo == null || indeksSelektovanogLevog == null) {
            Toast.makeText(requireContext(), "Prvo izaberite pojam levo!", Toast.LENGTH_SHORT).show()
            return
        }

        val resenja = if (trenutnaRunda == 1) resenjaRunda1 else resenjaRunda2
        val jeTacno = resenja[indeksSelektovanogLevog] == desniIndex

        if (jeTacno) {
            dodajPoene()
            oznaciStatus(selektovanoLevo!!, true)
            oznaciStatus(desnoDugme, true)
            spojenoLevo[indeksSelektovanogLevog!!] = true
            ukupnoSredjeno++
        } else {
            oznaciStatus(selektovanoLevo!!, false)
        }

        // Beležimo pokušaje
        if (activePlayer == leadPlayer) {
            pokusajiVodeceg++
        } else {
            pokusajiPopravnog++
        }

        selektovanoLevo = null
        indeksSelektovanogLevog = null

        proveriLogikuSledecegPoteza()
    }

    private fun proveriLogikuSledecegPoteza() {
        if (ukupnoSredjeno == 5) {
            sledecaRunda()
            return
        }

        if (activePlayer == leadPlayer && pokusajiVodeceg == 5) {
            switchPlayerInRound()
        }
        else if (activePlayer != leadPlayer && pokusajiPopravnog >= dozvoljenoPopravki) {
            sledecaRunda()
        }
    }

    private fun switchPlayerInRound() {
        activePlayer = if (leadPlayer == 1) 2 else 1
        timer?.cancel()

        dozvoljenoPopravki = 5 - ukupnoSredjeno
        pokusajiPopravnog = 0

        Toast.makeText(requireContext(), "Igrač $activePlayer popravlja!", Toast.LENGTH_LONG).show()

        val leftButtons = listOf(binding.btnLeft1, binding.btnLeft2, binding.btnLeft3, binding.btnLeft4, binding.btnLeft5)
        leftButtons.forEachIndexed { index, button ->
            if (!spojenoLevo[index]) {
                resetButton(button)
            }
        }

        zapocniTajmer()
    }

    private fun dodajPoene() {
        if (activePlayer == 1) {
            rezultatIgrac1 += 2
            binding.gameHeader.tvPlayer1Score.text = "Igrač 1\n$rezultatIgrac1"
        } else {
            rezultatIgrac2 += 2
            binding.gameHeader.tvPlayer2Score.text = "Igrač 2\n$rezultatIgrac2"
        }
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
                if (activePlayer == leadPlayer) switchPlayerInRound() else sledecaRunda()
            }
        }.start()
    }

    private fun sledecaRunda() {
        if (trenutnaRunda == 1) {
            trenutnaRunda = 2
            Toast.makeText(requireContext(), "Runda 2", Toast.LENGTH_LONG).show()
            popuniRundu()
        } else {
            zavrsiIgru()
        }
    }

    private fun zavrsiIgru() {
        timer?.cancel()
        Toast.makeText(requireContext(), "Kraj! P1: $rezultatIgrac1 | P2: $rezultatIgrac2", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    private fun resetButton(button: MaterialButton) {
        button.isEnabled = true
        button.backgroundTintList = defaultBackgroundTint
        button.setTextColor(defaultTextColors)
    }

    private fun oznaciSelektovano(button: MaterialButton) {
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary)
        button.backgroundTintList = ColorStateList.valueOf(primaryColor)
        button.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnPrimary))
    }

    private fun oznaciStatus(button: MaterialButton, isCorrect: Boolean) {
        button.isEnabled = false
        val colorRes = if (isCorrect) android.R.color.holo_green_light else android.R.color.holo_red_light
        button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), colorRes)
        button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}