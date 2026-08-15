package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.GardenDto
import com.example.releaf.data.remote.dto.GardenUpdateDto
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.data.repository.GardenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GardenViewModel : ViewModel() {
    private val repository = GardenRepository()

    private val _garden = MutableStateFlow<GardenDto?>(null)
    val garden: StateFlow<GardenDto?> = _garden.asStateFlow()

    private val _plantSlots = MutableStateFlow<List<PlantSlotDto>>(emptyList())
    val plantSlots: StateFlow<List<PlantSlotDto>> = _plantSlots.asStateFlow()

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

    fun loadGarden(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                _garden.value = repository.getGarden(userId)
                _plantSlots.value = repository.getPlantSlots(userId)
            } catch (_: Exception) { }
        }
    }

    private val authRepository = com.example.releaf.data.repository.AuthRepository()

    fun growPlant(userId: String) {
        viewModelScope.launch {
            try {
                val g = _garden.value ?: return@launch
                if (g.grow_uses_left <= 0) return@launch
                val emptySlot = _plantSlots.value.firstOrNull { it.state == "EMPTY_POT" } ?: return@launch

                val newExp = g.current_exp + 50
                val newPoints = g.current_points + 10
                val newGems = g.current_gems + 1
                val update = GardenUpdateDto(
                    current_exp = newExp,
                    exp_target = g.exp_target,
                    grow_uses_left = g.grow_uses_left - 1,
                    fertilize_uses_left = g.fertilize_uses_left,
                    current_points = newPoints,
                    current_gems = newGems
                )
                repository.updateGarden(userId, update)
                repository.updatePlantSlot(emptySlot.id, "GROWING")
                
                // Also update global total points
                val profile = authRepository.getProfile(userId)
                if (profile != null) {
                    authRepository.updateTotalPoints(userId, profile.total_points + 10)
                }
                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
                loadGarden(userId)
            } catch (e: Exception) {
                android.util.Log.e("GardenViewModel", "Operation failed", e)
            }
        }
    }

    fun fertilizePlant(userId: String) {
        viewModelScope.launch {
            try {
                val g = _garden.value ?: return@launch
                if (g.fertilize_uses_left <= 0) return@launch
                val growingSlot = _plantSlots.value.firstOrNull { it.state == "GROWING" } ?: return@launch

                val newExp = g.current_exp + 100
                val newPoints = g.current_points + 20
                val update = GardenUpdateDto(
                    current_exp = newExp,
                    exp_target = g.exp_target,
                    grow_uses_left = g.grow_uses_left,
                    fertilize_uses_left = g.fertilize_uses_left - 1,
                    current_points = newPoints,
                    current_gems = g.current_gems
                )
                repository.updateGarden(userId, update)
                repository.updatePlantSlot(growingSlot.id, "FULLY_GROWN")

                // Also update global total points
                val profile = authRepository.getProfile(userId)
                if (profile != null) {
                    authRepository.updateTotalPoints(userId, profile.total_points + 20)
                }
                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
                loadGarden(userId)
            } catch (e: Exception) {
                android.util.Log.e("GardenViewModel", "Operation failed", e)
            }
        }
    }

    fun harvestSlot(slotId: String, userId: String) {
        viewModelScope.launch {
            try {
                val g = _garden.value ?: return@launch
                val newPoints = g.current_points + 50
                val update = GardenUpdateDto(
                    current_exp = g.current_exp,
                    exp_target = g.exp_target,
                    grow_uses_left = g.grow_uses_left,
                    fertilize_uses_left = g.fertilize_uses_left,
                    current_points = newPoints,
                    current_gems = g.current_gems
                )
                repository.updateGarden(userId, update)
                repository.updatePlantSlot(slotId, "EMPTY_POT")

                // Also update global total points
                val profile = authRepository.getProfile(userId)
                if (profile != null) {
                    authRepository.updateTotalPoints(userId, profile.total_points + 50)
                }
                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
                loadGarden(userId)
            } catch (e: Exception) {
                android.util.Log.e("GardenViewModel", "Operation failed", e)
            }
        }
    }
}
