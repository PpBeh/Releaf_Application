package com.example.releaf.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.GardenDto
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.data.repository.AuthRepository
import com.example.releaf.data.repository.GardenRepository
import com.example.releaf.data.repository.QuestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class GardenViewModel : ViewModel() {
    private val repository = GardenRepository()
    private val authRepository = AuthRepository()
    private val questRepository = QuestRepository()

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
            com.example.releaf.data.remote.SupabaseModule.refreshEvent.collect {
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
                _plantSlots.value = repository.getPlantSlots(userId)

                val remoteExp = g?.current_exp ?: 0
                val remoteGems = g?.current_gems ?: 0
                val today = getTodayDateString()

                if (context != null) {
                    val prefs = context.getSharedPreferences("garden_prefs", Context.MODE_PRIVATE)
                    val localExp = prefs.getInt("tree_exp_${userId}", 0)

                    val actualExp = max(localExp, remoteExp)

                    _currentExp.value = actualExp
                    _currentGems.value = remoteGems

                    if (remoteExp > localExp) {
                        prefs.edit().putInt("tree_exp_${userId}", remoteExp).apply()
                    }

                    // 计算剩余次数
                    val maxAllowed = getMaxUsesByExp(actualExp)
                    val waterUsed = prefs.getInt("water_${userId}_$today", 0)
                    val fertilizeUsed = prefs.getInt("fertilize_${userId}_$today", 0)

                    _waterUsesLeft.value = (maxAllowed - waterUsed).coerceAtLeast(0)
                    _fertilizeUsesLeft.value = (maxAllowed - fertilizeUsed).coerceAtLeast(0)
                } else {
                    _currentExp.value = remoteExp
                    _currentGems.value = remoteGems
                    val maxAllowed = getMaxUsesByExp(remoteExp)
                    _waterUsesLeft.value = (g?.grow_uses_left ?: maxAllowed).coerceAtLeast(0)
                    _fertilizeUsesLeft.value = (g?.fertilize_uses_left ?: maxAllowed).coerceAtLeast(0)
                }
            } catch (e: Exception) {
                if (context != null) {
                    val prefs = context.getSharedPreferences("garden_prefs", Context.MODE_PRIVATE)
                    val localExp = prefs.getInt("tree_exp_${userId}", 0)
                    _currentExp.value = localExp

                    val maxAllowed = getMaxUsesByExp(localExp)
                    val today = getTodayDateString()
                    val waterUsed = prefs.getInt("water_${userId}_$today", 0)
                    val fertilizeUsed = prefs.getInt("fertilize_${userId}_$today", 0)
                    _waterUsesLeft.value = (maxAllowed - waterUsed).coerceAtLeast(0)
                    _fertilizeUsesLeft.value = (maxAllowed - fertilizeUsed).coerceAtLeast(0)
                }
            }
        }
    }

    fun waterPlant(userId: String, context: Context? = null) {
        val exp = _currentExp.value
        val maxAllowed = getMaxUsesByExp(exp)
        val today = getTodayDateString()

        var prefs: android.content.SharedPreferences? = null
        var waterUsed = 0

        if (context != null) {
            prefs = context.getSharedPreferences("garden_prefs", Context.MODE_PRIVATE)
            waterUsed = prefs.getInt("water_${userId}_$today", 0)
            if (waterUsed >= maxAllowed) {
                _statusMessage.value = "You've reached today's watering limit. Come back tomorrow!"
                return
            }
        }

        val newExp = exp + 50
        _currentExp.value = newExp

        if (prefs != null) {
            prefs.edit()
                .putInt("water_${userId}_$today", waterUsed + 1)
                .putInt("tree_exp_${userId}", newExp)
                .apply()
            _waterUsesLeft.value = (maxAllowed - (waterUsed + 1)).coerceAtLeast(0)
        }

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

        var prefs: android.content.SharedPreferences? = null
        var fertilizeUsed = 0

        if (context != null) {
            prefs = context.getSharedPreferences("garden_prefs", Context.MODE_PRIVATE)
            fertilizeUsed = prefs.getInt("fertilize_${userId}_$today", 0)
            if (fertilizeUsed >= maxAllowed) {
                _statusMessage.value = "You've reached today's fertilizing limit. Come back tomorrow!"
                return
            }
        }

        val newExp = exp + 50
        _currentExp.value = newExp

        if (prefs != null) {
            prefs.edit()
                .putInt("fertilize_${userId}_$today", fertilizeUsed + 1)
                .putInt("tree_exp_${userId}", newExp)
                .apply()
            _fertilizeUsesLeft.value = (maxAllowed - (fertilizeUsed + 1)).coerceAtLeast(0)
        }

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

                val targetState = if (actionType == "WATER") "EMPTY_POT" else "GROWING"
                val newState = if (actionType == "WATER") "GROWING" else "FULLY_GROWN"
                val slot = _plantSlots.value.firstOrNull { it.state == targetState }
                if (slot != null) {
                    repository.updatePlantSlot(slot.id, newState)
                }

                if (actionType == "FERTILIZE") {
                    questRepository.incrementQuestsByType(userId, "FERTILIZE")
                }

                val profile = authRepository.getProfile(userId)
                if (profile != null) {
                    authRepository.updateTotalPoints(userId, profile.total_points + pointsToAdd)
                }

                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun harvestSlot(slotId: String, userId: String) {
        viewModelScope.launch {
            try {
                val g = _garden.value ?: return@launch

                repository.upsertGardenExp(
                    userId = userId,
                    newExp = _currentExp.value,
                    newPoints = g.current_points + 50,
                    newGems = g.current_gems,
                    expTarget = g.exp_target,
                    waterUsesLeft = _waterUsesLeft.value,
                    fertilizeUsesLeft = _fertilizeUsesLeft.value
                )
                repository.updatePlantSlot(slotId, "EMPTY_POT")
                questRepository.incrementQuestsByType(userId, "HARVEST")

                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}