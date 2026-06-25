package rs.tim13.slagalica.asocijacije.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import rs.tim13.slagalica.asocijacije.model.AssociationsGameRepository
import rs.tim13.slagalica.core.model.GameConfig

/**
 * Pravi [AssociationsViewModel] iz repozitorijuma (sloj podataka) i [GameConfig]
 * koji koordinator (ili fragment u demo režimu) prosleđuje. Ovo je mesto na kome
 * partija/izazov/turnir ubacuje pravi izvor podataka i konfiguraciju igrača.
 */
class AssociationsViewModelFactory(
    private val repository: AssociationsGameRepository,
    private val config: GameConfig
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AssociationsViewModel(
            games = repository.getGames(),
            localPlayer = config.localPlayer,
            isSinglePlayer = config.isSinglePlayer,
            initialOpponentDisconnected = config.initialOpponentDisconnected
        ) as T
    }
}
