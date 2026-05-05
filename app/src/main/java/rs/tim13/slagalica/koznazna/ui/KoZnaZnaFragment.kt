package rs.tim13.slagalica.koznazna.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentKoZnaZnaBinding

class KoZnaZnaFragment : BaseFragment<FragmentKoZnaZnaBinding>(FragmentKoZnaZnaBinding::inflate) {

    // Hardkodovani podaci samo za demonstraciju GUI-ja (KT 1)
    private var trenutniIndeks = 0
    private val mockPitanja = listOf(
        Pair("Koji glumac tumači lik Popa u filmu 'Munje!'?", listOf("Nikola Đuričko", "Sergej Trifunović", "Boris Milivojević", "Nenad Jezdić")),
        Pair("Koji je glavni grad Australije?", listOf("Sidnej", "Melburn", "Kanbera", "Pern")),
        Pair("Koja planeta je najbliža Suncu?", listOf("Venera", "Zemlja", "Mars", "Merkur"))
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        popuniPitanje()

        val onAnswerClicked = View.OnClickListener {
            Toast.makeText(requireContext(), "Odgovor zabeležen!", Toast.LENGTH_SHORT).show()

            if (trenutniIndeks < mockPitanja.size - 1) {
                trenutniIndeks++
                popuniPitanje()
            } else {
                Toast.makeText(requireContext(), "Kraj igre Ko zna zna!", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnAnswerA.setOnClickListener(onAnswerClicked)
        binding.btnAnswerB.setOnClickListener(onAnswerClicked)
        binding.btnAnswerC.setOnClickListener(onAnswerClicked)
        binding.btnAnswerD.setOnClickListener(onAnswerClicked)
    }

    private fun popuniPitanje() {
        val trenutno = mockPitanja[trenutniIndeks]
        binding.tvQuestion.text = trenutno.first
        binding.btnAnswerA.text = trenutno.second[0]
        binding.btnAnswerB.text = trenutno.second[1]
        binding.btnAnswerC.text = trenutno.second[2]
        binding.btnAnswerD.text = trenutno.second[3]
    }
}