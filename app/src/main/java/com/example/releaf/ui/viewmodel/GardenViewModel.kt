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
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
                val g = repository.getGarden(userId)
                _garden.value = g

                val remoteSlots = repository.getPlantSlots(userId)

                val mergedSlots = (1..6).map { index ->
                    val remote = remoteSlots.find { it.slot_index == index }
                    val localState = gardenPrefs.getString("slot_${userId}_$index", null)?.let {
                        if (it == "PLANTED") "GROWING" else it
                    }
                    // Prioritize DB state when present and valid; fallback to prefs
                    val state = when {
                        remote != null && remote.state != "EMPTY_POT" -> remote.state
                        localState != null && localState != "EMPTY_POT" -> localState
                        else -> remote?.state ?: "EMPTY_POT"
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
                    gardenPrefs.edit().putInt("tree_exp_$userId", remoteExp).apply()
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

        gardenPrefs.edit()
            .putInt("water_${userId}_$today", waterUsed + 1)
            .putInt("tree_exp_$userId", newExp)
            .apply()
        _waterUsesLeft.value = (maxAllowed - (waterUsed + 1)).coerceAtLeast(0)

        _statusMessage.value = "Watering completed! (+50 EXP)"

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

        gardenPrefs.edit()
            .putInt("fertilize_${userId}_$today", fertilizeUsed + 1)
            .putInt("tree_exp_$userId", newExp)
            .apply()
        _fertilizeUsesLeft.value = (maxAllowed - (fertilizeUsed + 1)).coerceAtLeast(0)

        _statusMessage.value = "Fertilizing completed! (+50 EXP)"

        syncToCloud(userId, newExp, "FERTILIZE")
    }

    private fun syncToCloud(userId: String, newExp: Int, actionType: String) {
        viewModelScope.launch {
            try {
                val g = _garden.value

                val pointsToAdd = if (actionType == "WATER") 10 else 20
                val newPoints = (g?.current_points ?: 0) + pointsToAdd
                val newGems = (g?.current_gems ?: 0) + if (actionType == "WATER") 1 else 0

                repository.upsertGardenExp(
                    userId = userId,
                    newExp = newExp,
                    newPoints = newPoints,
                    newGems = newGems,
                    expTarget = g?.exp_target ?: 2000,
                    waterUsesLeft = _waterUsesLeft.value,
                    fertilizeUsesLeft = _fertilizeUsesLeft.value
                )

                // Also ensure DB row reflects current gem/point state via upsertGarden if needed
                // Keep local _garden in sync
                _garden.value = g?.copy(
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
                    authRepository.updateTotalPoints(userId, (profile.total_points ?: 0) + pointsToAdd)
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
                    repository.upsertGardenExp(
                        userId = userId,
                        newExp = _currentExp.value,
                        newPoints = g.current_points + 50,
                        newGems = g.current_gems,
                        expTarget = g.exp_target,
                        waterUsesLeft = _waterUsesLeft.value,
                        fertilizeUsesLeft = _fertilizeUsesLeft.value
                    )
                }
                if (slotId.isNotBlank()) {
                    repository.updatePlantSlot(slotId, "EMPTY_POT")
                } else {
                    // Fallback: find slot by user and delete via slot index if id missing
                    val slots = _plantSlots.value
                    val toClear = slots.find { it.id == slotId }
                    if (toClear != null) {
                        repository.upsertPlantSlot(userId, toClear.slot_index, "EMPTY_POT", null)
                        gardenPrefs.edit().remove("slot_${userId}_${toClear.slot_index}").apply()
                    }
                }
                // Also clear prefs for that slot if we can identify index
                val slot = _plantSlots.value.find { it.id == slotId }
                slot?.let { gardenPrefs.edit().remove("slot_${userId}_${it.slot_index}").apply() }

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
                } catch (_: Exception) { }
                gardenPrefs.edit().putString("slot_${userId}_$slotIndex", "GROWING").apply()
                _statusMessage.value = "Planted ${seed.nickname.ifBlank { seed.name }} in Slot $slotIndex! 🌱"
                loadGarden(userId)
                SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
                gardenPrefs.edit().putString("slot_${userId}_$slotIndex", "GROWING").apply()
                _statusMessage.value = "Planted ${SeedData.getSeedForSlot(slotIndex).name}! 🌱"
                loadGarden(userId)
            }
        }
    }

    fun deletePlantSlot(userId: String, slotIndex: Int) {
        viewModelScope.launch {
            try {
                repository.deletePlantSlot(userId, slotIndex)
                gardenPrefs.edit().remove("slot_${userId}_$slotIndex").apply()
                _plantSlots.value = _plantSlots.value.map {
                    if (it.slot_index == slotIndex) it.copy(state = "EMPTY_POT", plant_type = null, id = "") else it
                }
                _statusMessage.value = "Plant in slot $slotIndex removed"
                SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
                gardenPrefs.edit().remove("slot_${userId}_$slotIndex").apply()
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
                    try { repository.deletePlantSlot(userId, i) } catch (_: Exception) { }
                    gardenPrefs.edit().remove("slot_${userId}_$i").apply()
                }
                gardenPrefs.edit().remove("tree_exp_$userId").apply()
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
