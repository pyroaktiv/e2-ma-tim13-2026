package rs.tim13.slagalica.korakpokorak.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentKorakPoKorakBinding

class KorakPoKorakFragment : BaseFragment<FragmentKorakPoKorakBinding>(FragmentKorakPoKorakBinding::inflate) {

    private lateinit var stepCards: List<View>
    private lateinit var clueTexts: List<TextView>
    private lateinit var numberTexts: List<TextView>

    private val koraciRunda1 = listOf("Može biti bela, crna ili mlečna", "Često se poklanja za praznike", "Sadrži kakao i maslac", "Švajcarska je poznata po njoj", "Može biti sa lešnicima", "Topi se na suncu", "Slatkiš u kockicama")
    private val resenjeRunda1 = "ČOKOLADA"

    private val koraciRunda2 = listOf("Najveća je mačka na svetu", "Ima prepoznatljive pruge", "Odličan je plivač", "Živi u azijskim džunglama", "Simbol je snage i moći", "U Sibiru dostiže najveću veličinu", "Šir Kan je jedan od njih")
    private val resenjeRunda2 = "TIGAR"

    private var trenutnaRunda = 1
    private var leadPlayer = 1
    private var activePlayer = 1
    private var trenutniKorak = 0
    private var rezultatIgrac1 = 0
    private var rezultatIgrac2 = 0
    private var canGuess = true

    private var timer: CountDownTimer? = null
    private var isOpponentChance = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.gameHeader.tvPlayer1Score.text = "Igrač 1\n0"
        binding.gameHeader.tvPlayer2Score.text = "Igrač 2\n0"

        setupUI()
        zapocniRundu()

        binding.btnSubmitSolution.setOnClickListener {
            val solution = binding.etSolution.text.toString().trim()
            if (solution.isNotBlank()) {
                if (canGuess) {
                    proveriResenje(solution)
                } else {
                    Toast.makeText(requireContext(), "Čekajte sledeći trag!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupUI() {
        val stepIds = listOf(R.id.step1, R.id.step2, R.id.step3, R.id.step4, R.id.step5, R.id.step6, R.id.step7)
        val tempCards = mutableListOf<View>()
        val tempClues = mutableListOf<TextView>()
        val tempNumbers = mutableListOf<TextView>()

        stepIds.forEach { id ->
            val includedView = view?.findViewById<View>(id)
            if (includedView != null) {
                tempCards.add(includedView)
                tempClues.add(includedView.findViewById(R.id.tvStepClue))
                tempNumbers.add(includedView.findViewById(R.id.tvStepNumber))
            }
        }
        stepCards = tempCards
        clueTexts = tempClues
        numberTexts = tempNumbers
    }

    private fun zapocniRundu() {
        trenutniKorak = 0
        isOpponentChance = false
        canGuess = true
        activePlayer = if (trenutnaRunda == 1) 1 else 2
        leadPlayer = activePlayer

        binding.etSolution.text?.clear()
        binding.btnSubmitSolution.isEnabled = true

        // Sakrij sve i postavi zvezdice
        clueTexts.forEach { it.text = "***********" }
        numberTexts.forEach { it.text = "" }
        stepCards.forEach { it.visibility = View.INVISIBLE }

        // Odmah otvori prvi korak
        revealNextStep(0)
        trenutniKorak = 1

        zapocniTajmerRunde()
    }

    private fun zapocniTajmerRunde() {
        timer?.cancel()
        // Runda traje 70 sekundi. Prvi korak je otvoren na 70s.
        timer = object : CountDownTimer(70000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sekunde = (millisUntilFinished / 1000).toInt()
                binding.gameHeader.tvGameTimer.text = sekunde.toString()

                // Logika otvaranja: 60s -> drugi, 50s -> treći... 10s -> sedmi
                val protekloSekundi = 70 - sekunde
                val noviIndex = protekloSekundi / 10

                if (noviIndex > trenutniKorak - 1 && noviIndex < 7) {
                    revealNextStep(noviIndex)
                    trenutniKorak = noviIndex + 1
                    canGuess = true // Novo polje, novi pokušaj
                }
            }

            override fun onFinish() {
                binding.gameHeader.tvGameTimer.text = "0"
                prebaciNaSansuProtivnika()
            }
        }.start()
    }

    private fun revealNextStep(index: Int) {
        val koraci = if (trenutnaRunda == 1) koraciRunda1 else koraciRunda2
        if (index < stepCards.size) {
            clueTexts[index].text = koraci[index]
            numberTexts[index].text = "${index + 1}."
            stepCards[index].visibility = View.VISIBLE
        }
    }

    private fun proveriResenje(pokusaj: String) {
        val tacnoResenje = if (trenutnaRunda == 1) resenjeRunda1 else resenjeRunda2

        if (pokusaj.equals(tacnoResenje, ignoreCase = true)) {
            zavrsiRundu(true)
        } else {
            if (!isOpponentChance) {
                canGuess = false
                Toast.makeText(requireContext(), "Netačno! Čekajte sledeći trag.", Toast.LENGTH_SHORT).show()
            } else {
                zavrsiRundu(false)
            }
            binding.etSolution.text?.clear()
        }
    }

    private fun prebaciNaSansuProtivnika() {
        timer?.cancel()
        // Otkrij sve preostale korake da protivnik vidi sve tragove
        for (i in trenutniKorak until 7) {
            revealNextStep(i)
        }

        isOpponentChance = true
        canGuess = true
        activePlayer = if (leadPlayer == 1) 2 else 1

        Toast.makeText(requireContext(), "Protivnik (Igrač $activePlayer) pogađa!", Toast.LENGTH_LONG).show()

        timer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.gameHeader.tvGameTimer.text = (millisUntilFinished / 1000).toString()
            }
            override fun onFinish() {
                zavrsiRundu(false)
            }
        }.start()
    }

    private fun zavrsiRundu(pogodjeno: Boolean) {
        timer?.cancel()

        if (pogodjeno) {
            val poeni = if (isOpponentChance) 5 else (22 - (trenutniKorak * 2))

            if (activePlayer == 1) rezultatIgrac1 += poeni else rezultatIgrac2 += poeni

            updateScoresUI()
            Toast.makeText(requireContext(), "Tačno! +$poeni poena", Toast.LENGTH_SHORT).show()
        } else {
            val tacno = if (trenutnaRunda == 1) resenjeRunda1 else resenjeRunda2
            Toast.makeText(requireContext(), "Rešenje: $tacno", Toast.LENGTH_LONG).show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (trenutnaRunda == 1) {
                trenutnaRunda = 2
                zapocniRundu()
            } else {
                zavrsiIgru()
            }
        }, 2000)
    }

    private fun updateScoresUI() {
        binding.gameHeader.tvPlayer1Score.text = "Igrač 1\n$rezultatIgrac1"
        binding.gameHeader.tvPlayer2Score.text = "Igrač 2\n$rezultatIgrac2"
    }

    private fun zavrsiIgru() {
        Toast.makeText(requireContext(), "Kraj! P1: $rezultatIgrac1 | P2: $rezultatIgrac2", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}