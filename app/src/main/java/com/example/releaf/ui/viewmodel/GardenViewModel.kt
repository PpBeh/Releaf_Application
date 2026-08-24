package com.example.releaf.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.GardenDto
import com.example.releaf.data.remote.dto.GardenUpdateDto
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

    fun getTreeStage(): Int {
        return getTreeStage(_currentExp.value)
    }

    fun getMaxUsesByExp(exp: Int): Int {
        return getTreeStage(exp)
    }

    fun loadGarden(userId: String, context: Context? = null) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                val g = repository.getGarden(userId)
                _garden.value = g
                _plantSlots.value = repository.getPlantSlots(userId)

                if (g != null) {
                    _currentExp.value = g.current_exp
                    _currentGems.value = g.current_gems
                }

                val exp = _currentExp.value
                val maxAllowed = getMaxUsesByExp(exp)
                val today = getTodayDateString()

                if (context != null) {
                    val prefs = context.getSharedPreferences("garden_daily_prefs", Context.MODE_PRIVATE)
                    val waterUsed = prefs.getInt("water_${userId}_$today", 0)
                    val fertilizeUsed = prefs.getInt("fertilize_${userId}_$today", 0)
                    _waterUsesLeft.value = (maxAllowed - waterUsed).coerceAtLeast(0)
                    _fertilizeUsesLeft.value = (maxAllowed - fertilizeUsed).coerceAtLeast(0)
                } else {
                    _waterUsesLeft.value = (g?.grow_uses_left ?: maxAllowed).coerceAtLeast(0)
                    _fertilizeUsesLeft.value = (g?.fertilize_uses_left ?: maxAllowed).coerceAtLeast(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
            prefs = context.getSharedPreferences("garden_daily_prefs", Context.MODE_PRIVATE)
            waterUsed = prefs.getInt("water_${userId}_$today", 0)
            if (waterUsed >= maxAllowed) {
                _statusMessage.value = "You've reached today's watering limit. Come back tomorrow!"
                return
            }
        }

        if (prefs != null) {
            prefs.edit().putInt("water_${userId}_$today", waterUsed + 1).apply()
            _waterUsesLeft.value = (maxAllowed - (waterUsed + 1)).coerceAtLeast(0)
        }
        _currentExp.value += 50
        _statusMessage.value = "Watering completed! (+50 EXP)"

        viewModelScope.launch {
            try {
                val g = _garden.value ?: return@launch

                val newPoints = g.current_points + 10
                val update = GardenUpdateDto(
                    current_exp = _currentExp.value,
                    exp_target = g.exp_target,
                    grow_uses_left = _waterUsesLeft.value,
                    fertilize_uses_left = _fertilizeUsesLeft.value,
                    current_points = newPoints,
                    current_gems = _currentGems.value
                )
                repository.updateGarden(userId, update)

                val emptySlot = _plantSlots.value.firstOrNull { it.state == "EMPTY_POT" }
                if (emptySlot != null) {
                    repository.updatePlantSlot(emptySlot.id, "GROWING")
                }

                val profile = authRepository.getProfile(userId)
                if (profile != null) {
                    authRepository.updateTotalPoints(userId, profile.total_points + 10)
                }

                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
            prefs = context.getSharedPreferences("garden_daily_prefs", Context.MODE_PRIVATE)
            fertilizeUsed = prefs.getInt("fertilize_${userId}_$today", 0)
            if (fertilizeUsed >= maxAllowed) {
                _statusMessage.value = "You've reached today's fertilizing limit. Come back tomorrow!"
                return
            }
        }

        if (prefs != null) {
            prefs.edit().putInt("fertilize_${userId}_$today", fertilizeUsed + 1).apply()
            _fertilizeUsesLeft.value = (maxAllowed - (fertilizeUsed + 1)).coerceAtLeast(0)
        }
        _currentExp.value += 50
        _statusMessage.value = "Fertilizing completed! (+50 EXP)"

        viewModelScope.launch {
            try {
                val g = _garden.value ?: return@launch

                val newPoints = g.current_points + 20
                val update = GardenUpdateDto(
                    current_exp = _currentExp.value,
                    exp_target = g.exp_target,
                    grow_uses_left = _waterUsesLeft.value,
                    fertilize_uses_left = _fertilizeUsesLeft.value,
                    current_points = newPoints,
                    current_gems = _currentGems.value
                )
                repository.updateGarden(userId, update)

                val growingSlot = _plantSlots.value.firstOrNull { it.state == "GROWING" }
                if (growingSlot != null) {
                    repository.updatePlantSlot(growingSlot.id, "FULLY_GROWN")
                }
                questRepository.incrementQuestsByType(userId, "FERTILIZE")

                val profile = authRepository.getProfile(userId)
                if (profile != null) {
                    authRepository.updateTotalPoints(userId, profile.total_points + 20)
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
                val update = GardenUpdateDto(
                    current_exp = g.current_exp,
                    exp_target = g.exp_target,
                    grow_uses_left = g.grow_uses_left,
                    fertilize_uses_left = g.fertilize_uses_left,
                    current_points = g.current_points + 50,
                    current_gems = g.current_gems
                )
                repository.updateGarden(userId, update)
                repository.updatePlantSlot(slotId, "EMPTY_POT")

                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
                loadGarden(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}