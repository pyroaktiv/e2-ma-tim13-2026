package rs.tim13.slagalica.skocko.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import rs.tim13.slagalica.core.model.GameConfig
import rs.tim13.slagalica.skocko.data.SkockoGameRepository

/**
 * Pravi [SkockoViewModel] iz repozitorijuma tajnih kombinacija i [GameConfig].
 */
class SkockoViewModelFactory(
    private val repository: SkockoGameRepository,
    private val config: GameConfig
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SkockoViewModel(
            secrets = repository.getSecrets(),
            localPlayer = config.localPlayer,
            isSinglePlayer = config.isSinglePlayer,
            initialOpponentDisconnected = config.initialOpponentDisconnected
        ) as T
    }
}
