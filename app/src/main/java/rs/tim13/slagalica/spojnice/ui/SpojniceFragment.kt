package rs.tim13.slagalica.spojnice.ui

import android.os.Bundle
import android.view.View
import com.google.android.material.button.MaterialButton
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentSpojniceBinding

class SpojniceFragment : BaseFragment<FragmentSpojniceBinding>(FragmentSpojniceBinding::inflate) {

    private val likovi = listOf("Pop", "Mare", "Gojko Sisa", "Kata", "Policajac")
    private val glumci = listOf("Nebojša Glogovac", "Maja Mandžuka", "Sergej Trifunović", "Nikola Đuričko", "Boris Milivojević")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        val leftButtons = listOf(binding.btnLeft1, binding.btnLeft2, binding.btnLeft3, binding.btnLeft4, binding.btnLeft5)
        leftButtons.forEachIndexed { index, button ->
            button.text = likovi[index]
            podesiKlikZaSelekciju(button)
        }

        val rightButtons = listOf(binding.btnRight1, binding.btnRight2, binding.btnRight3, binding.btnRight4, binding.btnRight5)
        rightButtons.forEachIndexed { index, button ->
            button.text = glumci[index]
            podesiKlikZaSelekciju(button)
        }
    }

    private fun podesiKlikZaSelekciju(button: MaterialButton) {
        button.setOnClickListener {
            val primaryColor = requireContext().getColor(R.color.black) // Koristimo ugrađenu crnu kao privremeni vizuelni indikator
            button.setBackgroundColor(primaryColor)
            button.setTextColor(requireContext().getColor(R.color.white))
        }
    }
}