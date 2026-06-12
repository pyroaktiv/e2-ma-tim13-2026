package rs.tim13.slagalica.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import rs.tim13.slagalica.auth.data.api.AuthApiService
import rs.tim13.slagalica.auth.data.api.dto.LoginRequest
import rs.tim13.slagalica.auth.data.api.dto.RegisterRequest
import rs.tim13.slagalica.auth.data.api.dto.ResetPasswordRequest

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class LoginSuccess(val token: String) : AuthState()
    data class RegisterSuccess(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val api: AuthApiService) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.login(request)
                if (response.isSuccessful && response.body() != null) {
                    _authState.value = AuthState.LoginSuccess(response.body()!!.token)
                } else {
                    _authState.value = AuthState.Error(response.errorBody()?.string() ?: "Nepoznata greška")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Greška u komunikaciji sa serverom.")
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = api.register(request)
                if (response.isSuccessful && response.body() != null) {
                    _authState.value = AuthState.RegisterSuccess(response.body()!!.message)
                } else {
                    _authState.value = AuthState.Error(response.errorBody()?.string() ?: "Nepoznata greška")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Greška u komunikaciji sa serverom.")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}