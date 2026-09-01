package com.example.releaf.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.data.remote.dto.RewardTierDto
import com.example.releaf.data.remote.dto.UserRewardDto
import com.example.releaf.data.repository.GardenRepository
import com.example.releaf.data.repository.RewardRepository
import com.example.releaf.model.SeedData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

class RewardsViewModel(application: Application) : AndroidViewModel(application) {
    private val rewardRepository = RewardRepository()
    private val gardenRepository = GardenRepository()

    private val gardenPrefs = application.getSharedPreferences("garden_prefs", Context.MODE_PRIVATE)

    private val _tiers = MutableStateFlow<List<RewardTierDto>>(emptyList())
    val tiers: StateFlow<List<RewardTierDto>> = _tiers.asStateFlow()

    private val _userRewards = MutableStateFlow<List<UserRewardDto>>(emptyList())
    val userRewards: StateFlow<List<UserRewardDto>> = _userRewards.asStateFlow()

    private val _userPoints = MutableStateFlow(0)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _gardenSlots = MutableStateFlow<List<PlantSlotDto>>(emptyList())
    val gardenSlots: StateFlow<List<PlantSlotDto>> = _gardenSlots.asStateFlow()

    private val _claimStatus = MutableStateFlow<String?>(null)
    val claimStatus: StateFlow<String?> = _claimStatus.asStateFlow()

    private var currentUserId = ""

    init {
        viewModelScope.launch {
            SupabaseModule.refreshEvent.collect {
                if (currentUserId.isNotBlank()) {
                    loadRewards(currentUserId)
                }
            }
        }
    }

    fun loadRewards(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                val garden = gardenRepository.getGarden(userId)
                val remoteExp = garden?.current_exp ?: 0
                val localExp = gardenPrefs.getInt("tree_exp_${userId}", 0)
                val actualExp = max(localExp, remoteExp)

                _userPoints.value = actualExp

                _tiers.value = rewardRepository.getRewardTiers()
                _userRewards.value = rewardRepository.getUserRewards(userId)
                _gardenSlots.value = loadSlotsWithLocalFallback(userId)
            } catch (e: Exception) {
                Log.e("RewardsViewModel", "Failed to load rewards data", e)

                val localExp = gardenPrefs.getInt("tree_exp_${userId}", 0)
                _userPoints.value = localExp
                _gardenSlots.value = loadSlotsWithLocalFallback(userId)
            }
        }
    }

    private suspend fun loadSlotsWithLocalFallback(userId: String): List<PlantSlotDto> {
        val remoteSlots = rewardRepository.getGardenSlots(userId)
        return (1..6).map { index ->
            val remote = remoteSlots.find { it.slot_index == index }
            val localState = gardenPrefs.getString("slot_${userId}_$index", null)
            val state = if (remote != null && remote.state != "EMPTY_POT") {
                remote.state
            } else if (localState != null) {
                localState
            } else {
                remote?.state ?: "EMPTY_POT"
            }
            PlantSlotDto(
                id = remote?.id ?: "",
                user_id = userId,
                slot_index = index,
                state = state,
                plant_type = remote?.plant_type ?: SeedData.getSeedForSlot(index).name
            )
        }
    }

    fun unlockTier(userId: String, tierId: String) {
        viewModelScope.launch {
            rewardRepository.unlockTier(userId, tierId)
            _userRewards.value = rewardRepository.getUserRewards(userId)
        }
    }

    fun claimPlantReward(userId: String, slotIndex: Int) {
        val seed = SeedData.getSeedForSlot(slotIndex)
        viewModelScope.launch {
            try {
                gardenPrefs.edit().putString("slot_${userId}_$slotIndex", "PLANTED").apply()

                rewardRepository.claimPlantReward(userId, slotIndex, seed.name)
                _gardenSlots.value = loadSlotsWithLocalFallback(userId)
                _claimStatus.value = "🌱 ${seed.name} seedling added to your Garden Plot!"

                SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                Log.e("RewardsViewModel", "Failed to claim plant reward", e)
                gardenPrefs.edit().putString("slot_${userId}_$slotIndex", "PLANTED").apply()
                _gardenSlots.value = loadSlotsWithLocalFallback(userId)
                _claimStatus.value = "🌱 ${seed.name} seedling added to your Garden Plot!"
            }
        }
    }

    fun clearClaimStatus() {
        _claimStatus.value = null
    }
}
