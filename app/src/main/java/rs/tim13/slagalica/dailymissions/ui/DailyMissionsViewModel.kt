package rs.tim13.slagalica.dailymissions.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import rs.tim13.slagalica.core.network.socket.ServerMessage
import rs.tim13.slagalica.core.network.socket.SocketManager
import rs.tim13.slagalica.dailymissions.data.DailyMissionsRepositoryImpl
import rs.tim13.slagalica.dailymissions.model.DailyMissionsResponse

class DailyMissionsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = DailyMissionsRepositoryImpl(app.applicationContext)

    private val _missions = MutableLiveData<DailyMissionsResponse?>()
    val missions: LiveData<DailyMissionsResponse?> = _missions

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val socketIncoming: LiveData<ServerMessage> = SocketManager.incoming

    fun load() {
        _isLoading.value = true
        _error.value = null
        repository.get { data, err ->
            _isLoading.postValue(false)
            if (data != null) _missions.postValue(data)
            else _error.postValue(err)
        }
    }
}
