package rs.tim13.slagalica.spojnice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import rs.tim13.slagalica.core.model.GameConfig
import rs.tim13.slagalica.spojnice.data.SpojniceGameRepository

/**
 * Pravi [SpojniceViewModel] iz repozitorijuma rundi i [GameConfig].
 */
class SpojniceViewModelFactory(
    private val repository: SpojniceGameRepository,
    private val config: GameConfig
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SpojniceViewModel(
            rounds = repository.getRounds(),
            localPlayer = config.localPlayer,
            isSinglePlayer = config.isSinglePlayer,
            initialOpponentDisconnected = config.initialOpponentDisconnected
        ) as T
    }
}
