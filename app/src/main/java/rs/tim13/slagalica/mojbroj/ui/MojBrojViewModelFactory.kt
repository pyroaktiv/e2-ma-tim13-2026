package rs.tim13.slagalica.mojbroj.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import rs.tim13.slagalica.core.model.GameConfig
import rs.tim13.slagalica.mojbroj.data.MojBrojGameRepository

/**
 * Pravi [MojBrojViewModel] iz repozitorijuma zagonetki i [GameConfig].
 */
class MojBrojViewModelFactory(
    private val repository: MojBrojGameRepository,
    private val config: GameConfig
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MojBrojViewModel(
            rounds = repository.getRounds(),
            localPlayer = config.localPlayer,
            isSinglePlayer = config.isSinglePlayer,
            initialOpponentDisconnected = config.initialOpponentDisconnected
        ) as T
    }
}
