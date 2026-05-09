package rs.tim13.slagalica.koznazna.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentKoZnaZnaBinding

class KoZnaZnaFragment : BaseFragment<FragmentKoZnaZnaBinding>(FragmentKoZnaZnaBinding::inflate) {

    private var trenutniIndeks = 0
    private var timer: CountDownTimer? = null
    private var isAnswered = false

    private var trenutniRezultat = 0

    private val mockPitanja = listOf(
        Pair("Koji glumac tumači lik Popa u filmu 'Munje!'?", listOf("Nikola Đuričko", "Sergej Trifunović", "Boris Milivojević", "Nenad Jezdić")),
        Pair("Koji je glavni grad Australije?", listOf("Sidnej", "Melburn", "Kanbera", "Pert")),
        Pair("Koja planeta je najbliža Suncu?", listOf("Venera", "Zemlja", "Mars", "Merkur")),
        Pair("Ko je napisao roman 'Na Drini ćuprija'?", listOf("Miloš Crnjanski", "Ivo Andrić", "Meša Selimović", "Dobrica Ćosić")),
        Pair("Koja je najduža reka u Evropi?", listOf("Dunav", "Volga", "Sava", "Rajna"))
    )

    private val tacniOdgovori = listOf(1, 2, 3, 1, 1)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.gameHeader.tvPlayer1Score.text = "Player 1\n0"
        binding.gameHeader.tvPlayer2Score.text = "Player 2\n0"
        setupUI()
    }

    private fun setupUI() {
        popuniPitanje()

        val dugmici = listOf(binding.btnAnswerA, binding.btnAnswerB, binding.btnAnswerC, binding.btnAnswerD)

        dugmici.forEachIndexed { indexDugmeta, button ->
            button.setOnClickListener { clickedView ->
                if (isAnswered) return@setOnClickListener
                isAnswered = true
                timer?.cancel()

                val correctIndex = tacniOdgovori[trenutniIndeks]
                val clickedButton = clickedView as MaterialButton
                val defaultColor = clickedButton.backgroundTintList

                if (indexDugmeta == correctIndex) {
                    trenutniRezultat += 10
                    clickedButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_light)
                } else {
                    trenutniRezultat -= 5
                    clickedButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light)
                    dugmici[correctIndex].backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_light)
                }

                //binding.gameHeader.tvPlayer1Score.text = trenutniRezultat.toString()
                binding.gameHeader.tvPlayer1Score.text = "Player 1\n$trenutniRezultat"

                Handler(Looper.getMainLooper()).postDelayed({
                    dugmici.forEach { it.backgroundTintList = defaultColor }
                    sledecePitanje()
                }, 1000)
            }
        }
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

                    val dugmici = listOf(binding.btnAnswerA, binding.btnAnswerB, binding.btnAnswerC, binding.btnAnswerD)
                    val correctIndex = tacniOdgovori[trenutniIndeks]
                    val defaultColor = dugmici[correctIndex].backgroundTintList

                    dugmici[correctIndex].backgroundTintList = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_green_light)

                    Handler(Looper.getMainLooper()).postDelayed({
                        dugmici[correctIndex].backgroundTintList = defaultColor
                        sledecePitanje()
                    }, 1000)
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
        Toast.makeText(requireContext(), "Kraj! Osvojili ste: $trenutniRezultat poena", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}