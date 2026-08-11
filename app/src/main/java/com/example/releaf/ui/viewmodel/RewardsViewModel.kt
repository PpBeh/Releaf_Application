package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.RewardTierDto
import com.example.releaf.data.remote.dto.UserRewardDto
import com.example.releaf.data.repository.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RewardsViewModel : ViewModel() {
    private val repository = RewardRepository()

    private val _tiers = MutableStateFlow<List<RewardTierDto>>(emptyList())
    val tiers: StateFlow<List<RewardTierDto>> = _tiers.asStateFlow()

    private val _userRewards = MutableStateFlow<List<UserRewardDto>>(emptyList())
    val userRewards: StateFlow<List<UserRewardDto>> = _userRewards.asStateFlow()

    private val _userPoints = MutableStateFlow(0)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    fun loadRewards(userId: String) {
        viewModelScope.launch {
            _tiers.value = repository.getRewardTiers()
            _userRewards.value = repository.getUserRewards(userId)
        }
    }

    fun setUserPoints(points: Int) {
        _userPoints.value = points
    }

    fun unlockTier(userId: String, tierId: String) {
        viewModelScope.launch {
            repository.unlockTier(userId, tierId)
            _userRewards.value = repository.getUserRewards(userId)
        }
    }
}
