package rs.tim13.slagalica.koznazna.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import rs.tim13.slagalica.core.model.GameConfig
import rs.tim13.slagalica.koznazna.data.KoZnaZnaGameRepository

/**
 * Pravi [KoZnaZnaViewModel] iz repozitorijuma pitanja i [GameConfig].
 */
class KoZnaZnaViewModelFactory(
    private val repository: KoZnaZnaGameRepository,
    private val config: GameConfig
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return KoZnaZnaViewModel(
            questions = repository.getQuestions(),
            localPlayer = config.localPlayer,
            isSinglePlayer = config.isSinglePlayer,
            initialOpponentDisconnected = config.initialOpponentDisconnected
        ) as T
    }
}
