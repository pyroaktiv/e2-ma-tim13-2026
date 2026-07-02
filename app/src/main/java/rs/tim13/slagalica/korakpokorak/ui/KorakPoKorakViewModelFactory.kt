package rs.tim13.slagalica.korakpokorak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import rs.tim13.slagalica.core.model.GameConfig
import rs.tim13.slagalica.korakpokorak.data.KorakPoKorakGameRepository

/**
 * Pravi [KorakPoKorakViewModel] iz repozitorijuma rundi i [GameConfig].
 */
class KorakPoKorakViewModelFactory(
    private val repository: KorakPoKorakGameRepository,
    private val config: GameConfig
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return KorakPoKorakViewModel(
            rounds = repository.getRounds(),
            localPlayer = config.localPlayer,
            isSinglePlayer = config.isSinglePlayer,
            initialOpponentDisconnected = config.initialOpponentDisconnected
        ) as T
    }
}
