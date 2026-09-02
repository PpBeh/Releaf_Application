package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.AchievementDto
import com.example.releaf.data.remote.dto.GardenDto
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.data.remote.dto.ProfileDto
import com.example.releaf.data.remote.dto.ProfileUpdateDto
import com.example.releaf.data.remote.dto.UserAchievementDto
import com.example.releaf.data.repository.AuthRepository
import com.example.releaf.data.repository.GardenRepository
import com.example.releaf.data.repository.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val rewardRepository = RewardRepository()
    private val gardenRepository = GardenRepository()

    private val _profile = MutableStateFlow<ProfileDto?>(null)
    val profile: StateFlow<ProfileDto?> = _profile.asStateFlow()

    private val _achievements = MutableStateFlow<List<UserAchievementDto>>(emptyList())
    val achievements: StateFlow<List<UserAchievementDto>> = _achievements.asStateFlow()

    private val _allAchievements = MutableStateFlow<List<AchievementDto>>(emptyList())
    val allAchievements: StateFlow<List<AchievementDto>> = _allAchievements.asStateFlow()

    private val _totalAchievements = MutableStateFlow(0)
    val totalAchievements: StateFlow<Int> = _totalAchievements.asStateFlow()

    private val _userGarden = MutableStateFlow<GardenDto?>(null)
    val userGarden: StateFlow<GardenDto?> = _userGarden.asStateFlow()

    private val _userPlantSlots = MutableStateFlow<List<PlantSlotDto>>(emptyList())
    val userPlantSlots: StateFlow<List<PlantSlotDto>> = _userPlantSlots.asStateFlow()

    private var currentUserId = ""

    init {
        viewModelScope.launch {
            com.example.releaf.data.remote.SupabaseModule.refreshEvent.collect {
                if (currentUserId.isNotBlank()) {
                    loadProfile(currentUserId)
                }
            }
        }
    }

    fun loadProfile(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                if (userId.isNotBlank()) {
                    _profile.value = authRepository.getProfile(userId)
                }
            } catch (_: Exception) { }

            try {
                _userGarden.value = gardenRepository.getGarden(userId)
            } catch (_: Exception) { }

            try {
                val slots = gardenRepository.getPlantSlots(userId)
                _userPlantSlots.value = slots
            } catch (_: Exception) { }

            try {
                _achievements.value = rewardRepository.getUserAchievements(userId)
            } catch (_: Exception) { }

            try {
                val all = rewardRepository.getAllAchievements()
                _allAchievements.value = all
                _totalAchievements.value = if (all.isNotEmpty()) all.size else rewardRepository.getTotalAchievements()
            } catch (_: Exception) { }
        }
    }

    fun updateProfile(userId: String, name: String, phone: String, avatarUrl: String) {
        viewModelScope.launch {
            try {
                authRepository.updateProfile(userId, ProfileUpdateDto(name, phone, avatarUrl))
                _profile.value = authRepository.getProfile(userId)
            } catch (_: Exception) { }
        }
    }

    fun updateTitle(userId: String, newTitle: String) {
        viewModelScope.launch {
            try {
                authRepository.updateTitle(userId, newTitle)
                _profile.value = authRepository.getProfile(userId)
            } catch (_: Exception) { }
        }
    }

    fun updateAvatarFrame(userId: String, frameId: String) {
        viewModelScope.launch {
            try {
                authRepository.updateAvatarFrame(userId, frameId)
                _profile.value = authRepository.getProfile(userId)
            } catch (_: Exception) { }
        }
    }

    fun uploadAvatar(userId: String, uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val url = authRepository.uploadAvatar(userId, uri, context)
                if (url != null) {
                    _profile.value = authRepository.getProfile(userId)
                }
            } catch (_: Exception) { }
        }
    }

    fun uploadBanner(userId: String, uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            try {
                val url = authRepository.uploadBanner(userId, uri, context)
                if (url != null) {
                    _profile.value = authRepository.getProfile(userId)
                }
            } catch (_: Exception) { }
        }
    }

    fun deletePlantSlot(userId: String, slotIndex: Int) {
        viewModelScope.launch {
            try {
                gardenRepository.deletePlantSlot(userId, slotIndex)
                // Also clear local fallback prefs if any
                // reload to reflect DB truth
                loadProfile(userId)
                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "deletePlantSlot failed", e)
            }
        }
    }

    fun deleteGarden(userId: String) {
        viewModelScope.launch {
            try {
                gardenRepository.deleteGarden(userId)
                for (i in 1..6) {
                    try { gardenRepository.deletePlantSlot(userId, i) } catch (_: Exception) { }
                }
                loadProfile(userId)
                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "deleteGarden failed", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
