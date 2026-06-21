package rs.tim13.slagalica.match

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MatchViewModelFactory(
    private val appContext: Context,
    private val mode: MatchMode
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MatchViewModel(appContext, mode) as T
}
