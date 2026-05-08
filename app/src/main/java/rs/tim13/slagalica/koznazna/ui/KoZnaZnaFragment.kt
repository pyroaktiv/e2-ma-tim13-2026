package rs.tim13.slagalica.koznazna.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentKoZnaZnaBinding

class KoZnaZnaFragment : BaseFragment<FragmentKoZnaZnaBinding>(FragmentKoZnaZnaBinding::inflate) {

    private var trenutniIndeks = 0
    private var timer: CountDownTimer? = null
    private var isAnswered = false

    private val mockPitanja = listOf(
        Pair("Koji glumac tumači lik Popa u filmu 'Munje!'?", listOf("Nikola Đuričko", "Sergej Trifunović", "Boris Milivojević", "Nenad Jezdić")),
        Pair("Koji je glavni grad Australije?", listOf("Sidnej", "Melburn", "Kanbera", "Pert")),
        Pair("Koja planeta je najbliža Suncu?", listOf("Venera", "Zemlja", "Mars", "Merkur")),
        Pair("Ko je napisao roman 'Na Drini ćuprija'?", listOf("Miloš Crnjanski", "Ivo Andrić", "Meša Selimović", "Dobrica Ćosić")),
        Pair("Koja je najduža reka u Evropi?", listOf("Dunav", "Volga", "Sava", "Rajna"))
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        popuniPitanje()

        val onAnswerClicked = View.OnClickListener { clickedView ->
            if (isAnswered) return@OnClickListener
            isAnswered = true
            timer?.cancel()
            val defaultColor = clickedView.backgroundTintList
            clickedView.backgroundTintList = requireContext().getColorStateList(R.color.black)

            Handler(Looper.getMainLooper()).postDelayed({
                clickedView.backgroundTintList = defaultColor
                sledecePitanje()
            }, 500)
        }

        binding.btnAnswerA.setOnClickListener(onAnswerClicked)
        binding.btnAnswerB.setOnClickListener(onAnswerClicked)
        binding.btnAnswerC.setOnClickListener(onAnswerClicked)
        binding.btnAnswerD.setOnClickListener(onAnswerClicked)
    }

    private fun popuniPitanje() {
        isAnswered = false
        val trenutno = mockPitanja[trenutniIndeks]
        binding.tvQuestion.text = "${trenutniIndeks + 1}. ${trenutno.first}"
        binding.btnAnswerA.text = trenutno.second[0]
        binding.btnAnswerB.text = trenutno.second[1]
        binding.btnAnswerC.text = trenutno.second[2]
        binding.btnAnswerD.text = trenutno.second[3]

        zapocniTajmer()
    }

    private fun zapocniTajmer() {
        timer?.cancel()
        binding.gameHeader.tvGameTimer.text = "5"

        timer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.gameHeader.tvGameTimer.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                binding.gameHeader.tvGameTimer.text = "0"
                if (!isAnswered) {
                    isAnswered = true
                    sledecePitanje()
                }
            }
        }.start()
    }

    private fun sledecePitanje() {
        if (trenutniIndeks < mockPitanja.size - 1) {
            trenutniIndeks++
            popuniPitanje()
        } else {
            zavrsiIgru()
        }
    }

    private fun zavrsiIgru() {
        timer?.cancel()
        Toast.makeText(requireContext(), "Kraj igre Ko zna zna!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}