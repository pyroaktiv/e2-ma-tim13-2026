package rs.tim13.slagalica.match

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import rs.tim13.slagalica.core.network.socket.MatchContentDto

class MatchViewModelFactory(
    private val appContext: Context,
    private val mode: MatchMode,
    private val challengeId: String? = null,
    private val tournamentMatchId: String? = null,
    private val tournamentColor: String? = null,
    private val tournamentContent: MatchContentDto? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MatchViewModel(appContext, mode, challengeId, tournamentMatchId, tournamentColor, tournamentContent) as T
}
