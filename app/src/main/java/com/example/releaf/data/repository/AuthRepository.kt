package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.ProfileDto
import com.example.releaf.data.remote.dto.ProfileUpdateDto
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository {
    private val client = SupabaseModule.client
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = client.auth.currentUserOrNull()?.id ?: return Result.failure(Exception("No user after login"))
            _sessionState.value = SessionState.LoggedIn(userId)
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<String> {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val userId = client.auth.currentUserOrNull()?.id
            if (userId != null) {
                updateProfileName(userId, name)
                createGarden(userId)
                _sessionState.value = SessionState.LoggedIn(userId)
                Result.success(userId)
            } else {
                Result.success("pending_verification")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendVerificationEmail(email: String, password: String) {
        try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (_: Exception) { }
    }

    suspend fun logout() {
        client.auth.signOut()
        _sessionState.value = SessionState.LoggedOut
    }

    fun getCurrentUserId(): String? {
        return client.auth.currentSessionOrNull()?.user?.id
    }

    suspend fun restoreSession(): Boolean {
        return try {
            client.auth.retrieveUserForCurrentSession()
            val userId = client.auth.currentUserOrNull()?.id ?: return false
            _sessionState.value = SessionState.LoggedIn(userId)
            true
        } catch (e: Exception) {
            _sessionState.value = SessionState.LoggedOut
            false
        }
    }

    private suspend fun updateProfileName(userId: String, name: String) {
        try {
            client.postgrest.from("profiles").update(
                mapOf("name" to name)
            ) { filter { eq("id", userId) } }
        } catch (_: Exception) { }
    }

    private suspend fun createGarden(userId: String) {
        try {
            client.postgrest.from("gardens").insert(
                mapOf("user_id" to userId)
            )
            for (i in 1..6) {
                client.postgrest.from("plant_slots").insert(
                    mapOf("user_id" to userId, "slot_index" to i, "state" to "EMPTY_POT")
                )
            }
        } catch (_: Exception) { }
    }

    suspend fun updateProfile(userId: String, update: ProfileUpdateDto) {
        client.postgrest.from("profiles")
            .update(update) { filter { eq("id", userId) } }
    }

    suspend fun getProfile(userId: String): ProfileDto? {
        return client.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull()
    }
}

sealed class SessionState {
    data object LoggedOut : SessionState()
    data class LoggedIn(val userId: String) : SessionState()
}
