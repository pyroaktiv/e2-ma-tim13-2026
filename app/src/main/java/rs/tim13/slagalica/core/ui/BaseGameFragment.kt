package rs.tim13.slagalica.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewbinding.ViewBinding

abstract class BaseGameFragment<VB : ViewBinding, S : GameUiState, VM : BaseGameViewModel<S>>(
    inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : BaseFragment<VB>(inflate) {

    protected abstract val viewModel: VM

    protected abstract val tvTimer: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            renderCommonState(state)
            renderSpecificState(state)
        }

        // Ako nas hostuje orkestrator partije, vežemo igru na njega: potezi i kraj igre idu
        // ka koordinatoru, a protivnikovi potezi stižu nazad kroz coordinator.attachGame.
        (parentFragment as? GameCoordinatorHost)?.gameCoordinator?.let { coordinator ->
            coordinator.attachGame(viewModel)
            viewModel.events.observe(viewLifecycleOwner) { event ->
                when (event) {
                    is GameEvent.MovePlayed -> coordinator.onLocalMove(event.action, event.payload)
                    is GameEvent.GameFinished -> coordinator.onGameFinished(event.result)
                }
            }
        }
    }

    protected abstract fun setupUI()

    protected abstract fun renderSpecificState(state: S)

    private fun renderCommonState(state: S) {
        tvTimer.text = state.remainingSeconds.toString()
    }
}