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

    private val _userGems = MutableStateFlow(0)
    val userGems: StateFlow<Int> = _userGems.asStateFlow()

    private val _walletPoints = MutableStateFlow(0)
    val walletPoints: StateFlow<Int> = _walletPoints.asStateFlow()

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
                _userGems.value = garden?.current_gems ?: gardenPrefs.getInt("gems_${userId}", 0)
                _walletPoints.value = garden?.current_points ?: 0

                _tiers.value = rewardRepository.getRewardTiers()
                _userRewards.value = rewardRepository.getUserRewards(userId)
                _gardenSlots.value = loadSlotsWithLocalFallback(userId)

                // Check and notify available rewards
                try {
                    val notifRepo = com.example.releaf.data.repository.NotificationRepository()
                    val existing = notifRepo.getNotifications(userId)
                    SeedData.seedList.forEach { seed ->
                        if (actualExp >= seed.targetPoints) {
                            val title = "🎁 Reward Available: ${seed.name}"
                            if (existing.none { it.title == title }) {
                                notifRepo.sendNotification(
                                    userId = userId,
                                    title = title,
                                    body = "Congratulations! You reached ${seed.targetPoints} points and unlocked the ${seed.name} seed. Claim it now!",
                                    type = "REWARD"
                                )
                            }
                        }
                    }
                } catch (_: Exception) { }
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
            val rawLocal = gardenPrefs.getString("slot_${userId}_$index", null)
            val localState = if (rawLocal == "PLANTED") "GROWING" else rawLocal
            val remoteState = remote?.state?.let { if (it == "PLANTED") "GROWING" else it }
            // Server row is the source of truth when it exists; local prefs only
            // fill in when the slot has never been synced.
            val state = when {
                remote != null -> remoteState ?: "EMPTY_POT"
                localState != null && localState != "EMPTY_POT" -> localState
                else -> "EMPTY_POT"
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
                rewardRepository.claimPlantReward(userId, slotIndex, seed.name)
                gardenPrefs.edit().putString("slot_${userId}_$slotIndex", "GROWING").apply()
                _gardenSlots.value = loadSlotsWithLocalFallback(userId)
                _claimStatus.value = "🌱 ${seed.name} seedling added to your Garden Plot!"

                SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                Log.e("RewardsViewModel", "Failed to claim plant reward", e)
                _gardenSlots.value = loadSlotsWithLocalFallback(userId)
                _claimStatus.value = "Could not add the ${seed.name} seedling. Check your connection and try again."
            }
        }
    }

    fun clearClaimStatus() {
        _claimStatus.value = null
    }

    fun equipTitle(userId: String, title: String) {
        viewModelScope.launch {
            try {
                com.example.releaf.data.repository.AuthRepository().updateTitle(userId, title)
                _claimStatus.value = "Title equipped: $title"
                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
            } catch (_: Exception) {}
        }
    }

    fun purchaseFrame(userId: String, frameName: String, gemPrice: Int, pointPrice: Int) {
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("frames_$userId", android.content.Context.MODE_PRIVATE)
                if (prefs.getBoolean("owned_$frameName", false)) {
                    _claimStatus.value = "You already own the $frameName frame."
                    return@launch
                }
                // Always validate against the freshest server balances.
                val garden = gardenRepository.getGarden(userId)
                    ?: run {
                        _claimStatus.value = "Garden data not available yet. Please try again in a moment."
                        return@launch
                    }
                if (garden.current_points < pointPrice || garden.current_gems < gemPrice) {
                    _claimStatus.value = "Not enough Points/Gems"
                    return@launch
                }
                gardenRepository.updateGarden(userId, com.example.releaf.data.remote.dto.GardenUpdateDto(
                    current_exp = garden.current_exp,
                    exp_target = garden.exp_target,
                    grow_uses_left = garden.grow_uses_left,
                    fertilize_uses_left = garden.fertilize_uses_left,
                    current_points = (garden.current_points - pointPrice).coerceAtLeast(0),
                    current_gems = (garden.current_gems - gemPrice).coerceAtLeast(0)
                ))
                prefs.edit().putBoolean("owned_$frameName", true).apply()
                com.example.releaf.data.repository.AuthRepository().updateAvatarFrame(userId, frameName)
                _userPoints.value = garden.current_exp
                _userGems.value = (garden.current_gems - gemPrice).coerceAtLeast(0)
                _walletPoints.value = (garden.current_points - pointPrice).coerceAtLeast(0)
                _claimStatus.value = "Purchased $frameName frame!"
                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                _claimStatus.value = "Purchase failed: ${e.message}"
            }
        }
    }
}
