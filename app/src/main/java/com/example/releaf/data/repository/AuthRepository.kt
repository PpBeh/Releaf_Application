package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.ProfileDto
import com.example.releaf.data.remote.dto.ProfileUpdateDto
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
            val user = client.auth.currentUserOrNull() ?: return Result.failure(Exception("No user after login"))

            val emailVerified = user.emailConfirmedAt != null
            if (!emailVerified) {
                client.auth.signOut()
                return Result.failure(Exception("Email not verified. Please click the confirmation link in your email, then try again."))
            }

            applyPendingName(user.id)
            _sessionState.value = SessionState.LoggedIn(user.id)
            Result.success(user.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<String> {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("name", name)
                }
            }

            val currentSession = client.auth.currentSessionOrNull()

            if (currentSession != null) {
                val userId = currentSession.user?.id ?: return Result.failure(Exception("No user id"))
                updateProfileName(userId, name)
                createGarden(userId)
                _sessionState.value = SessionState.LoggedIn(userId)
                Result.success(userId)
            } else {
                com.example.releaf.data.remote.DeepLinkHolder.pendingName = name
                Result.success("pending_verification")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun applyPendingName(userId: String) {
        val pendingName = com.example.releaf.data.remote.DeepLinkHolder.pendingName
        if (pendingName != null && pendingName.isNotBlank()) {
            updateProfileName(userId, pendingName)
            com.example.releaf.data.remote.DeepLinkHolder.pendingName = null
        }
    }

    suspend fun resendVerificationEmail(email: String) {
        try {
            client.auth.resendEmail(type = OtpType.Email.SIGNUP, email = email)
        } catch (_: Exception) { }
    }

    @Deprecated("Use resendVerificationEmail(email) instead")
    suspend fun resendVerificationEmail(email: String, password: String) {
        resendVerificationEmail(email)
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            client.auth.resetPasswordForEmail(email = email, redirectUrl = "releaf://login-callback")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importSession(accessToken: String, refreshToken: String): Boolean {
        return try {
            val session = io.github.jan.supabase.auth.user.UserSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = 3600,
                expiresAt = kotlinx.datetime.Instant.fromEpochSeconds(System.currentTimeMillis() / 1000 + 3600),
                tokenType = "bearer",
                user = null
            )
            client.auth.importSession(session)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updatePassword(newPassword: String) {
        client.auth.updateUser {
            password = newPassword
        }
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
            try {
                client.auth.loadFromStorage()
            } catch (_: Exception) { }
            val localSession = client.auth.currentSessionOrNull()
            if (localSession != null) {
                val userId = localSession.user?.id ?: return false
                _sessionState.value = SessionState.LoggedIn(userId)
                true
            } else {
                try {
                    client.auth.retrieveUserForCurrentSession()
                    val userId = client.auth.currentUserOrNull()?.id ?: return false
                    _sessionState.value = SessionState.LoggedIn(userId)
                    true
                } catch (_: Exception) {
                    _sessionState.value = SessionState.LoggedOut
                    false
                }
            }
        } catch (e: Exception) {
            _sessionState.value = SessionState.LoggedOut
            false
        }
    }

    private suspend fun updateProfileName(userId: String, name: String) {
        try {
            client.postgrest.from("profiles").upsert(
                mapOf("id" to userId, "name" to name)
            )
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

    suspend fun updateTitle(userId: String, newTitle: String) {
        try {
            client.postgrest.from("profiles").update(
                mapOf("title" to newTitle)
            ) { filter { eq("id", userId) } }
        } catch (_: Exception) { }
    }

    suspend fun updateAvatarFrame(userId: String, frameId: String) {
        try {
            client.postgrest.from("profiles").update(
                mapOf("avatar_frame" to frameId)
            ) { filter { eq("id", userId) } }
        } catch (_: Exception) { }
    }

    suspend fun updateTotalPoints(userId: String, newTotal: Int) {
        try {
            client.postgrest.from("profiles").update(
                mapOf("total_points" to newTotal)
            ) { filter { eq("id", userId) } }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error updating points", e)
        }
    }

    suspend fun getProfile(userId: String): ProfileDto? {
        return client.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull()
    }

    suspend fun uploadAvatar(userId: String, uri: android.net.Uri, context: android.content.Context): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val fileName = "${userId}_${System.currentTimeMillis()}.jpg"
            client.storage.from("avatars").upload(
                path = fileName,
                data = bytes
            ) {
                upsert = true
            }
            val url = client.storage.from("avatars").publicUrl(fileName)
            client.postgrest.from("profiles").update(
                mapOf("avatar_url" to url)
            ) { filter { eq("id", userId) } }
            url
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadBanner(userId: String, uri: android.net.Uri, context: android.content.Context): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val fileName = "banner_${userId}_${System.currentTimeMillis()}.jpg"
            client.storage.from("avatars").upload(
                path = fileName,
                data = bytes
            ) {
                upsert = true
            }
            val url = client.storage.from("avatars").publicUrl(fileName)
            client.postgrest.from("profiles").update(
                mapOf("banner_url" to url)
            ) { filter { eq("id", userId) } }
            url
        } catch (_: Exception) {
            null
        }
    }

    suspend fun completeProfileSetup(userId: String, name: String) {
        updateProfileName(userId, name)
        createGarden(userId)
    }
}

sealed class SessionState {
    data object LoggedOut : SessionState()
    data class LoggedIn(val userId: String) : SessionState()
}
