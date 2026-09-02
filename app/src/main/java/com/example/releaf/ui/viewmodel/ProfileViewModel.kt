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

class ProfileViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val rewardRepository = RewardRepository()
    private val gardenRepository = GardenRepository()
    private val gardenPrefs = application.getSharedPreferences("garden_prefs", android.content.Context.MODE_PRIVATE)

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

    private val _userReviewCount = MutableStateFlow(0)
    val userReviewCount: StateFlow<Int> = _userReviewCount.asStateFlow()

    private val _userVerifiedCount = MutableStateFlow(0)
    val userVerifiedCount: StateFlow<Int> = _userVerifiedCount.asStateFlow()

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
                val remoteSlots = try { gardenRepository.getPlantSlots(userId) } catch (_: Exception) { emptyList() }
                val merged = (1..6).map { idx ->
                    val remote = remoteSlots.find { it.slot_index == idx }
                    val rawLocal = gardenPrefs.getString("slot_${userId}_$idx", null)?.let { if (it == "PLANTED") "GROWING" else it }
                    // Server row is the source of truth when it exists; local prefs only
                    // fill in when the slot has never been synced.
                    val state = when {
                        remote != null -> remote.state
                        rawLocal != null && rawLocal != "EMPTY_POT" -> rawLocal
                        else -> "EMPTY_POT"
                    }
                    com.example.releaf.data.remote.dto.PlantSlotDto(
                        id = remote?.id ?: "",
                        user_id = userId,
                        slot_index = idx,
                        state = state,
                        plant_type = remote?.plant_type ?: com.example.releaf.model.SeedData.getSeedForSlot(idx).name
                    )
                }
                _userPlantSlots.value = merged
            } catch (_: Exception) { }

            try {
                _userReviewCount.value = com.example.releaf.data.repository.ReviewRepository().countUserReviews(userId)
            } catch (_: Exception) { }

            try {
                _userVerifiedCount.value = com.example.releaf.data.repository.PoiRepository().countUserVerifications(userId)
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

    fun updateAvatarFrame(userId: String, frameId: String) {
        val current = _profile.value
        if (current != null) {
            _profile.value = current.copy(avatar_frame = frameId)
        }
        viewModelScope.launch {
            try {
                authRepository.updateAvatarFrame(userId, frameId)
                _profile.value = authRepository.getProfile(userId)
            } catch (_: Exception) { }
        }
    }

    fun deletePlantSlot(userId: String, slotIndex: Int) {
        viewModelScope.launch {
            try {
                gardenRepository.deletePlantSlot(userId, slotIndex)
                gardenPrefs.edit().remove("slot_${userId}_$slotIndex").apply()
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
                    gardenPrefs.edit().remove("slot_${userId}_$i").apply()
                }
                gardenPrefs.edit().remove("tree_exp_$userId").apply()
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
