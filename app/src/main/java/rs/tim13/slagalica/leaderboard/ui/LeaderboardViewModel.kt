package rs.tim13.slagalica.leaderboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import rs.tim13.slagalica.leaderboard.data.LeaderboardRepositoryImpl
import rs.tim13.slagalica.leaderboard.model.LeaderboardResponse

class LeaderboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = LeaderboardRepositoryImpl(app.applicationContext)

    private val _currentTab = MutableLiveData(0)
    val currentTab: LiveData<Int> = _currentTab

    private val _leaderboardState = MutableLiveData<LeaderboardResponse?>()
    val leaderboardState: LiveData<LeaderboardResponse?> = _leaderboardState

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun load(tab: Int) {
        _currentTab.value = tab
        _isLoading.value = true
        _error.value = null
        val callback = { data: LeaderboardResponse?, err: String? ->
            _isLoading.postValue(false)
            if (data != null) _leaderboardState.postValue(data)
            else _error.postValue(err)
        }
        if (tab == 0) repository.getWeekly(callback)
        else repository.getMonthly(callback)
    }

    fun refresh() {
        load(_currentTab.value ?: 0)
    }
}
