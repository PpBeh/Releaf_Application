package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.DeepLinkHolder
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

    private val _needsPasswordReset = MutableStateFlow(false)
    val needsPasswordReset: StateFlow<Boolean> = _needsPasswordReset.asStateFlow()

    private val _registeredName = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val restored = repository.restoreSession()
            _session.value = if (restored) {
                val userId = repository.getCurrentUserId() ?: ""
                SessionState.LoggedIn(userId)
            } else {
                SessionState.LoggedOut
            }

            handleDeepLinkTokens()

            val elapsed = System.currentTimeMillis() - startTime
            val minSplash = 2500L
            if (elapsed < minSplash) {
                kotlinx.coroutines.delay(minSplash - elapsed)
            }
            _isCheckingSession.value = false
        }
    }

    fun handleDeepLink() {
        viewModelScope.launch {
            handleDeepLinkTokens()
        }
    }

    private suspend fun handleDeepLinkTokens() {
        val token = DeepLinkHolder.accessToken
        val refresh = DeepLinkHolder.refreshToken
        val type = DeepLinkHolder.type
        if (token != null && refresh != null) {
            val imported = repository.importSession(token, refresh)
            if (imported) {
                val userId = repository.getCurrentUserId() ?: ""
                if (type == "recovery") {
                    _needsPasswordReset.value = true
                    _session.value = SessionState.LoggedIn(userId)
                } else if (type == "signup") {
                    val name = _registeredName.value ?: DeepLinkHolder.pendingName ?: "User"
                    repository.completeProfileSetup(userId, name)
                    _registeredName.value = null
                    DeepLinkHolder.pendingName = null
                    _session.value = SessionState.LoggedIn(userId)
                } else {
                    _session.value = SessionState.LoggedIn(userId)
                }
            }
            DeepLinkHolder.clear()
        }
    }

    fun updatePassword(newPassword: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.updatePassword(newPassword)
                _needsPasswordReset.value = false
                onDone()
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Failed to update password")
            }
        }
    }

    fun clearAuthSuccess() {
        _uiState.value = AuthUiState()
    }
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = repository.login(email, password)
            result.fold(
                onSuccess = { userId ->
                    val pendingName = _registeredName.value
                    if (pendingName != null) {
                        repository.completeProfileSetup(userId, pendingName)
                        _registeredName.value = null
                    }

                    _uiState.value = AuthUiState(isLoginSuccess = true)
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
            _registeredName.value = name

            val result = repository.register(name, email, password)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState(isRegistrationSuccess = true, registeredEmail = email)
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
            val name = _registeredName.value ?: "User"

            val result = repository.login(email, password)
            result.fold(
                onSuccess = { userId ->
                    repository.completeProfileSetup(userId, name)

                    _session.value = SessionState.LoggedIn(userId)
                    _registeredPassword.value = null
                    _registeredName.value = null
                    _uiState.value = AuthUiState(isLoginSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState(error = "Email not verified yet. Check your inbox, click the confirmation link, then tap Continue.")
                }
            )
        }
    }

    fun resendVerification(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.resendVerificationEmail(email)
                _uiState.value = AuthUiState(resendSuccess = true, resendMessage = "Verification email resent. Please check your inbox.")
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = e.message ?: "Failed to resend email")
            }
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

    private val _resetState = MutableStateFlow(ResetUiState())
    val resetState: StateFlow<ResetUiState> = _resetState.asStateFlow()

    fun sendResetEmail(email: String) {
        viewModelScope.launch {
            _resetState.value = ResetUiState(isLoading = true)
            val result = repository.resetPassword(email)
            result.fold(
                onSuccess = {
                    _resetState.value = ResetUiState(isSuccess = true)
                },
                onFailure = { error ->
                    _resetState.value = ResetUiState(error = error.message ?: "Failed to send reset email")
                }
            )
        }
    }

    fun clearResetState() {
        _resetState.value = ResetUiState()
    }

    fun getCurrentUserId(): String? = repository.getCurrentUserId()
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false,
    val isRegistrationSuccess: Boolean = false,
    val error: String? = null,
    val registeredEmail: String = "",
    val resendSuccess: Boolean = false,
    val resendMessage: String? = null
)

data class ResetUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
