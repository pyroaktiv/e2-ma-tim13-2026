package rs.tim13.slagalica.mojbroj.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.ui.BaseGameFragment
import rs.tim13.slagalica.databinding.FragmentMojBrojBinding
import rs.tim13.slagalica.match.MatchHost
import kotlin.math.sqrt

class MojBrojFragment :
    BaseGameFragment<FragmentMojBrojBinding, MojBrojUiState, MojBrojViewModel>(FragmentMojBrojBinding::inflate) {

    private val host get() = requireParentFragment() as MatchHost

    override val viewModel: MojBrojViewModel by viewModels {
        MojBrojViewModelFactory(host.match.mojBrojRepository(), host.match.gameConfig)
    }

    override val tvTimer: TextView get() = binding.gameHeader.tvGameTimer

    private lateinit var numberButtons: List<Button>

    // Shake senzor za „stop" (spec 6.l)
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeMs = 0L
    private var canStopNow = false

    override fun setupUI() {
        numberButtons = listOf(
            binding.btnNum1, binding.btnNum2, binding.btnNum3,
            binding.btnNum4, binding.btnNum5, binding.btnNum6
        )

        binding.btnStop.setOnClickListener { viewModel.requestStop() }
        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.addNumber(index) }
        }
        binding.btnPlus.setOnClickListener { viewModel.addOperator("+") }
        binding.btnMinus.setOnClickListener { viewModel.addOperator("-") }
        binding.btnMultiply.setOnClickListener { viewModel.addOperator("*") }
        binding.btnDivide.setOnClickListener { viewModel.addOperator("/") }
        binding.btnOpenBracket.setOnClickListener { viewModel.addOpenBracket() }
        binding.btnCloseBracket.setOnClickListener { viewModel.addCloseBracket() }
        binding.btnBackspace.setOnClickListener { viewModel.backspace() }
        binding.btnSubmit.setOnClickListener { viewModel.submitSolution() }

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun renderSpecificState(state: MojBrojUiState) {
        canStopNow = state.canStop

        binding.gameHeader.tvPlayer1Score.text = getString(R.string.game_player1_score, state.blueScore)
        binding.gameHeader.tvPlayer2Score.text = getString(R.string.game_player2_score, state.redScore)
        binding.tvRoundLabel.text = getString(R.string.game_round_label, state.round)

        binding.tvTargetNumber.text = state.target?.toString() ?: "???"
        binding.tvCurrentExpression.text = state.expressionDisplay
        binding.tvStatusMessage.text = state.statusMessage

        numberButtons.forEachIndexed { index, button ->
            button.text = state.numbers?.getOrNull(index)?.toString() ?: "?"
            button.isEnabled = state.isSolving && index !in state.usedNumberIndices
        }
        listOf(
            binding.btnPlus, binding.btnMinus, binding.btnMultiply, binding.btnDivide,
            binding.btnOpenBracket, binding.btnCloseBracket, binding.btnBackspace
        ).forEach { it.isEnabled = state.isSolving }
        binding.btnSubmit.isEnabled = state.isSolving && state.isExpressionComplete

        binding.btnStop.visibility = if (state.canStop) View.VISIBLE else View.GONE
        val solvingVisibility = if (state.isSolving) View.VISIBLE else View.GONE
        binding.llOfferedNumbers.visibility = solvingVisibility
        binding.glOperations.visibility = solvingVisibility
        binding.btnSubmit.visibility = solvingVisibility
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager?.registerListener(shakeListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeListener)
    }

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!canStopNow) return
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
            val now = System.currentTimeMillis()
            if (gForce > SHAKE_THRESHOLD && now - lastShakeMs > SHAKE_DEBOUNCE_MS) {
                lastShakeMs = now
                viewModel.requestStop()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    companion object {
        private const val SHAKE_THRESHOLD = 2.7f
        private const val SHAKE_DEBOUNCE_MS = 1000L
    }
}
