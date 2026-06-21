package rs.tim13.slagalica.core.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import rs.tim13.slagalica.R
import rs.tim13.slagalica.core.model.Player
import rs.tim13.slagalica.databinding.LayoutGameHeaderBinding

abstract class BaseGameFragment<VB : ViewBinding, S : GameUiState, VM : BaseGameViewModel<S>>(
    inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : BaseFragment<VB>(inflate) {

    protected abstract val viewModel: VM

    /** Include binding za "layout_game_header.xml", zajednički za sve igre. */
    protected abstract val gameHeader: LayoutGameHeaderBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        applyPlayerBackground()

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

    /** Pozadina cele partije nosi blagu nijansu boje lokalnog igrača (plava za Igrača 1, crvena za Igrača 2). */
    private fun applyPlayerBackground() {
        val colorRes = if (viewModel.localPlayer == Player.BLUE) R.color.match_bg_player_blue else R.color.match_bg_player_red
        binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    protected abstract fun setupUI()

    protected abstract fun renderSpecificState(state: S)

    private fun renderCommonState(state: S) {
        gameHeader.tvGameTimer.text = state.remainingSeconds.toString()
        highlightActivePlayer(state.activePlayer)
        renderTokensAndStars()
    }

    /** Tokeni/zvezde dolaze iz [GameConfig] koordinatora partije — isti izvor kao na home ekranu. */
    private fun renderTokensAndStars() {
        val config = (parentFragment as? GameCoordinatorHost)?.gameCoordinator?.gameConfig ?: return
        gameHeader.tvHeaderTokens.text = getString(R.string.home_tokens, config.tokens)
        gameHeader.tvHeaderStars.text = getString(R.string.home_stars, config.stars)
    }

    private fun highlightActivePlayer(activePlayer: Player) {
        val (active, inactive) = if (activePlayer == Player.BLUE) {
            gameHeader.tvPlayer1Score to gameHeader.tvPlayer2Score
        } else {
            gameHeader.tvPlayer2Score to gameHeader.tvPlayer1Score
        }
        active.alpha = 1f
        active.setTypeface(active.typeface, Typeface.BOLD)
        inactive.alpha = 0.5f
        inactive.setTypeface(inactive.typeface, Typeface.NORMAL)
    }
}