package com.example.releaf.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.GardenDto
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.data.repository.AuthRepository
import com.example.releaf.data.repository.GardenRepository
import com.example.releaf.data.repository.QuestRepository
import com.example.releaf.data.repository.RewardRepository
import com.example.releaf.model.SeedData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import androidx.core.content.edit

class GardenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GardenRepository()
    private val authRepository = AuthRepository()
    private val questRepository = QuestRepository()
    private val rewardRepository = RewardRepository()

    private val gardenPrefs = application.getSharedPreferences("garden_prefs", Context.MODE_PRIVATE)

    private val _garden = MutableStateFlow<GardenDto?>(null)
    val garden: StateFlow<GardenDto?> = _garden.asStateFlow()

    private val _plantSlots = MutableStateFlow<List<PlantSlotDto>>(emptyList())
    val plantSlots: StateFlow<List<PlantSlotDto>> = _plantSlots.asStateFlow()

    private val _currentExp = MutableStateFlow(0)
    val currentExp: StateFlow<Int> = _currentExp.asStateFlow()

    private val _currentGems = MutableStateFlow(0)
    val currentGems: StateFlow<Int> = _currentGems.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _waterUsesLeft = MutableStateFlow(1)
    val waterUsesLeft: StateFlow<Int> = _waterUsesLeft.asStateFlow()

    private val _fertilizeUsesLeft = MutableStateFlow(1)
    val fertilizeUsesLeft: StateFlow<Int> = _fertilizeUsesLeft.asStateFlow()

    private var currentUserId = ""

    init {
        viewModelScope.launch {
            SupabaseModule.refreshEvent.collect {
                if (currentUserId.isNotBlank()) {
                    loadGarden(currentUserId)
                }
            }
        }
    }

    private fun getTodayDateString(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        return fmt.format(Date())
    }

    fun getTreeStage(exp: Int): Int {
        return when {
            exp >= 5000 -> 3
            exp >= 2000 -> 2
            else -> 1
        }
    }

    fun getTreeStage(): Int = getTreeStage(_currentExp.value)
    fun getMaxUsesByExp(exp: Int): Int = getTreeStage(exp)

    fun loadGarden(userId: String, context: Context? = null) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                val g =
                    repository.getGarden(userId)?.let { repository.healNegativeBalance(userId, it) }
                _garden.value = g

                var remoteSlots = repository.getPlantSlots(userId)

                // Push any plant that currently exists only on this device (planted
                // while offline or before RLS allowed writes) up to the server so it
                // survives reinstalls.
                val remoteByIndex = remoteSlots.associateBy { it.slot_index }
                var pushedAny = false
                for (index in 1..6) {
                    val localRaw = gardenPrefs.getString("slot_${userId}_$index", null)
                    val localState = if (localRaw == "PLANTED") "GROWING" else localRaw
                    if (localState == null || localState == "EMPTY_POT") continue
                    val remote = remoteByIndex[index]
                    if (remote == null || remote.state == "EMPTY_POT") {
                        try {
                            repository.upsertPlantSlot(
                                userId = userId,
                                slotIndex = index,
                                state = localState,
                                plantType = SeedData.getSeedForSlot(index).name
                            )
                            pushedAny = true
                        } catch (_: Exception) {
                        }
                    }
                }
                if (pushedAny) {
                    remoteSlots = repository.getPlantSlots(userId)
                }

                val mergedSlots = (1..6).map { index ->
                    val remote = remoteSlots.find { it.slot_index == index }
                    val localState = gardenPrefs.getString("slot_${userId}_$index", null)?.let {
                        if (it == "PLANTED") "GROWING" else it
                    }
                    // Server row is the source of truth when it exists; local prefs
                    // only fill in when the slot has never been synced (offline first use).
                    val state = when {
                        remote != null -> remote.state
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
                _plantSlots.value = mergedSlots

                val remoteExp = g?.current_exp ?: 0
                val remoteGems = g?.current_gems ?: 0
                val today = getTodayDateString()

                val localExp = gardenPrefs.getInt("tree_exp_$userId", 0)
                val actualExp = max(localExp, remoteExp)

                _currentExp.value = actualExp
                _currentGems.value = remoteGems

                if (remoteExp > localExp) {
                    gardenPrefs.edit { putInt("tree_exp_$userId", remoteExp) }
                }

                val maxAllowed = getMaxUsesByExp(actualExp)
                val waterUsed = gardenPrefs.getInt("water_${userId}_$today", 0)
                val fertilizeUsed = gardenPrefs.getInt("fertilize_${userId}_$today", 0)

                _waterUsesLeft.value = (maxAllowed - waterUsed).coerceAtLeast(0)
                _fertilizeUsesLeft.value = (maxAllowed - fertilizeUsed).coerceAtLeast(0)
            } catch (e: Exception) {
                val localExp = gardenPrefs.getInt("tree_exp_$userId", 0)
                _currentExp.value = localExp

                val mergedSlots = (1..6).map { index ->
                    val localState = gardenPrefs.getString("slot_${userId}_$index", null)?.let {
                        if (it == "PLANTED") "GROWING" else it
                    }
                    PlantSlotDto(
                        id = "",
                        user_id = userId,
                        slot_index = index,
                        state = localState ?: "EMPTY_POT",
                        plant_type = SeedData.getSeedForSlot(index).name
                    )
                }
                _plantSlots.value = mergedSlots

                val maxAllowed = getMaxUsesByExp(localExp)
                val today = getTodayDateString()
                val waterUsed = gardenPrefs.getInt("water_${userId}_$today", 0)
                val fertilizeUsed = gardenPrefs.getInt("fertilize_${userId}_$today", 0)
                _waterUsesLeft.value = (maxAllowed - waterUsed).coerceAtLeast(0)
                _fertilizeUsesLeft.value = (maxAllowed - fertilizeUsed).coerceAtLeast(0)
            }
        }
    }

    fun waterPlant(userId: String, context: Context? = null) {
        val exp = _currentExp.value
        val maxAllowed = getMaxUsesByExp(exp)
        val today = getTodayDateString()

        val waterUsed = gardenPrefs.getInt("water_${userId}_$today", 0)
        if (waterUsed >= maxAllowed) {
            _statusMessage.value = "You've reached today's watering limit. Come back tomorrow!"
            return
        }

        val newExp = exp + 50
        _currentExp.value = newExp

        gardenPrefs.edit {
            putInt("water_${userId}_$today", waterUsed + 1)
                .putInt("tree_exp_$userId", newExp)
        }
        _waterUsesLeft.value = (maxAllowed - (waterUsed + 1)).coerceAtLeast(0)

        _statusMessage.value = "Watering completed! (+50 EXP, +10 🪙 Points, +1 💎 Gem)"

        syncToCloud(userId, newExp, "WATER")
    }

    fun growPlant(userId: String, context: Context? = null) {
        waterPlant(userId, context)
    }

    fun fertilizePlant(userId: String, context: Context? = null) {
        val exp = _currentExp.value
        val maxAllowed = getMaxUsesByExp(exp)
        val today = getTodayDateString()

        val fertilizeUsed = gardenPrefs.getInt("fertilize_${userId}_$today", 0)
        if (fertilizeUsed >= maxAllowed) {
            _statusMessage.value = "You've reached today's fertilizing limit. Come back tomorrow!"
            return
        }

        val newExp = exp + 50
        _currentExp.value = newExp

        gardenPrefs.edit {
            putInt("fertilize_${userId}_$today", fertilizeUsed + 1)
                .putInt("tree_exp_$userId", newExp)
        }
        _fertilizeUsesLeft.value = (maxAllowed - (fertilizeUsed + 1)).coerceAtLeast(0)

        _statusMessage.value = "Fertilizing completed! (+50 EXP, +20 🪙 Points)"

        syncToCloud(userId, newExp, "FERTILIZE")
    }

    private fun syncToCloud(userId: String, newExp: Int, actionType: String) {
        viewModelScope.launch {
            try {
                // Always base point/gem arithmetic on the freshest server row so a
                // stale/empty local copy can never zero out the account totals.
                val fresh = try {
                    repository.getGarden(userId)?.let { repository.healNegativeBalance(userId, it) }
                } catch (_: Exception) {
                    null
                } ?: _garden.value

                val pointsToAdd = if (actionType == "WATER") 10 else 20
                val gemsToAdd = if (actionType == "WATER") 1 else 0
                val newPoints = (fresh?.current_points ?: 0) + pointsToAdd
                val newGems = (fresh?.current_gems ?: 0) + gemsToAdd

                repository.upsertGardenExp(
                    userId = userId,
                    newExp = newExp,
                    newPoints = newPoints,
                    newGems = newGems,
                    expTarget = fresh?.exp_target ?: 2000,
                    waterUsesLeft = _waterUsesLeft.value,
                    fertilizeUsesLeft = _fertilizeUsesLeft.value
                )

                // Also ensure DB row reflects current gem/point state via upsertGarden if needed
                // Keep local _garden in sync
                _garden.value = fresh?.copy(
                    current_exp = newExp,
                    current_points = newPoints,
                    current_gems = newGems
                ) ?: GardenDto(
                    user_id = userId,
                    current_exp = newExp,
                    current_points = newPoints,
                    current_gems = newGems
                )

                if (actionType == "FERTILIZE") {
                    questRepository.incrementQuestsByType(userId, "FERTILIZE")
                } else if (actionType == "WATER") {
                    questRepository.incrementQuestsByType(userId, "WATER")
                }

                val profile = authRepository.getProfile(userId)
                if (profile != null) {
                    authRepository.updateTotalPoints(
                        userId,
                        (profile.total_points ?: 0) + pointsToAdd
                    )
                }

                SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun harvestSlot(slotId: String, userId: String) {
        viewModelScope.launch {
            try {
                val g = _garden.value ?: repository.getGarden(userId)

                if (g != null) {
                    val fresh = repository.getGarden(userId) ?: g
                    repository.upsertGardenExp(
                        userId = userId,
                        newExp = _currentExp.value,
                        newPoints = fresh.current_points + 50,
                        newGems = fresh.current_gems,
                        expTarget = fresh.exp_target,
                        waterUsesLeft = _waterUsesLeft.value,
                        fertilizeUsesLeft = _fertilizeUsesLeft.value
                    )
                }
                val targetSlot = if (slotId.isNotBlank()) {
                    _plantSlots.value.find { it.id == slotId }
                } else {
                    // Fallback: clear the first planted slot when the id is missing
                    _plantSlots.value.firstOrNull { it.state != "EMPTY_POT" }
                }
                if (targetSlot != null) {
                    repository.deletePlantSlot(userId, targetSlot.slot_index)
                    gardenPrefs.edit { remove("slot_${userId}_${targetSlot.slot_index}") }
                }

                questRepository.incrementQuestsByType(userId, "HARVEST")

                loadGarden(userId)
                SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun plantSeed(slotIndex: Int, userId: String) {
        viewModelScope.launch {
            try {
                val seed = SeedData.getSeedForSlot(slotIndex)
                // Use GROWING (valid CHECK) instead of PLANTED
                repository.upsertPlantSlot(userId, slotIndex, "GROWING", seed.name)
                try {
                    rewardRepository.claimPlantReward(userId, slotIndex, seed.name)
                } catch (_: Exception) {
                }
                gardenPrefs.edit { putString("slot_${userId}_$slotIndex", "GROWING") }
                _statusMessage.value =
                    "Planted ${seed.nickname.ifBlank { seed.name }} in Slot $slotIndex! 🌱"
                loadGarden(userId)
                SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
                gardenPrefs.edit { putString("slot_${userId}_$slotIndex", "GROWING") }
                _statusMessage.value = "Planted ${SeedData.getSeedForSlot(slotIndex).name}! 🌱"
                loadGarden(userId)
            }
        }
    }

    fun deletePlantSlot(userId: String, slotIndex: Int) {
        viewModelScope.launch {
            try {
                repository.deletePlantSlot(userId, slotIndex)
                gardenPrefs.edit { remove("slot_${userId}_$slotIndex") }
                _plantSlots.value = _plantSlots.value.map {
                    if (it.slot_index == slotIndex) it.copy(
                        state = "EMPTY_POT",
                        plant_type = null,
                        id = ""
                    ) else it
                }
                _statusMessage.value = "Plant in slot $slotIndex removed"
                SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
                gardenPrefs.edit { remove("slot_${userId}_$slotIndex") }
                loadGarden(userId)
            }
        }
    }

    fun deleteGarden(userId: String) {
        viewModelScope.launch {
            try {
                repository.deleteGarden(userId)
                // Also delete all plant slots
                for (i in 1..6) {
                    try {
                        repository.deletePlantSlot(userId, i)
                    } catch (_: Exception) {
                    }
                    gardenPrefs.edit { remove("slot_${userId}_$i") }
                }
                gardenPrefs.edit { remove("tree_exp_$userId") }
                _garden.value = null
                _plantSlots.value = emptyList()
                _currentExp.value = 0
                _currentGems.value = 0
                _statusMessage.value = "Garden removed"
                SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
