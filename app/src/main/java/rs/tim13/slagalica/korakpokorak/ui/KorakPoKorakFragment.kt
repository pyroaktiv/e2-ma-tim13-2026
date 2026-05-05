package rs.tim13.slagalica.korakpokorak.ui

import android.os.Bundle
import android.view.View
import rs.tim13.slagalica.core.ui.BaseFragment
import rs.tim13.slagalica.databinding.FragmentKorakPoKorakBinding

class KorakPoKorakFragment : BaseFragment<FragmentKorakPoKorakBinding>(FragmentKorakPoKorakBinding::inflate) {

    private lateinit var stepViews: List<View>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        binding.btnSubmitSolution.setOnClickListener {
            val solution = binding.etSolution.text.toString()
            if (solution.isNotBlank()) {
                TODO()
            }
        }
    }

    private fun setupUI() {

    }

    fun revealNextStep(stepIndex: Int, clue: String) {

    }
}