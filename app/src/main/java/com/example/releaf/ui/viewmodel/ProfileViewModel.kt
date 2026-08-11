package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.ProfileDto
import com.example.releaf.data.remote.dto.ProfileUpdateDto
import com.example.releaf.data.remote.dto.UserAchievementDto
import com.example.releaf.data.repository.AuthRepository
import com.example.releaf.data.repository.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val rewardRepository = RewardRepository()

    private val _profile = MutableStateFlow<ProfileDto?>(null)
    val profile: StateFlow<ProfileDto?> = _profile.asStateFlow()

    private val _achievements = MutableStateFlow<List<UserAchievementDto>>(emptyList())
    val achievements: StateFlow<List<UserAchievementDto>> = _achievements.asStateFlow()

    private val _totalAchievements = MutableStateFlow(0)
    val totalAchievements: StateFlow<Int> = _totalAchievements.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            try {
                if (userId.isNotBlank()) {
                    _profile.value = authRepository.getProfile(userId)
                }
            } catch (_: Exception) { }
            try {
                _achievements.value = rewardRepository.getUserAchievements(userId)
            } catch (_: Exception) { }
            try {
                _totalAchievements.value = rewardRepository.getTotalAchievements()
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

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
