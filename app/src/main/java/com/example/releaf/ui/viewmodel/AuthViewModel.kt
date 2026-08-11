package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.repository.AuthRepository
import com.example.releaf.data.repository.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _session = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(true)
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    init {
        viewModelScope.launch {
            val restored = repository.restoreSession()
            _session.value = if (restored) {
                val userId = repository.getCurrentUserId() ?: ""
                SessionState.LoggedIn(userId)
            } else {
                SessionState.LoggedOut
            }
            _isCheckingSession.value = false
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { userId ->
                    _uiState.value = AuthUiState(isSuccess = true)
                    _session.value = SessionState.LoggedIn(userId)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState(error = error.message ?: "Login failed")
                }
            )
        }
    }

    private val _registeredPassword = MutableStateFlow<String?>(null)

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            _registeredPassword.value = password
            val result = repository.register(name, email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState(isSuccess = true, registeredEmail = email)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState(error = error.message ?: "Registration failed")
                }
            )
        }
    }

    fun verifyEmail(email: String, code: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val password = _registeredPassword.value ?: ""
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { userId ->
                    _session.value = SessionState.LoggedIn(userId)
                    _registeredPassword.value = null
                    _uiState.value = AuthUiState(isSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState(error = "Email not verified yet. Check your inbox, click the confirmation link, then tap Continue.")
                }
            )
        }
    }

    fun resendVerification(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val password = _registeredPassword.value ?: ""
            repository.resendVerificationEmail(email, password)
            _uiState.value = AuthUiState()
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _session.value = SessionState.LoggedOut
            _uiState.value = AuthUiState()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getCurrentUserId(): String? = repository.getCurrentUserId()
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val registeredEmail: String = ""
)
