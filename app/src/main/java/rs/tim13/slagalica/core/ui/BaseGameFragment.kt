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
    }

    protected abstract fun setupUI()

    protected abstract fun renderSpecificState(state: S)

    private fun renderCommonState(state: S) {
        tvTimer.text = state.remainingSeconds.toString()
    }
}