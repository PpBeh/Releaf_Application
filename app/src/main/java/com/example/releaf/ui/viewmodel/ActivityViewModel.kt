package com.example.releaf.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.UserQuestDto
import com.example.releaf.data.remote.dto.UserQuestUpdateDto
import com.example.releaf.data.repository.QuestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuestRepository()
    private val prefs = application.getSharedPreferences("activity_prefs", Context.MODE_PRIVATE)

    private val _userQuests = MutableStateFlow<List<UserQuestDto>>(emptyList())
    val userQuests: StateFlow<List<UserQuestDto>> = _userQuests.asStateFlow()

    fun loadQuests(userId: String) {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val lastRefresh = prefs.getString("last_refresh_$userId", "")

                repository.incrementQuestsByType(userId, "CHECK_IN")

                val userQuests = repository.getUserQuests(userId)
                
                if (today != lastRefresh) {
                    // Daily refresh logic
                    val easyQuests = repository.getQuestsByDifficulty("EASY")
                    val medQuests = repository.getQuestsByDifficulty("MEDIUM")
                    val hardQuests = repository.getQuestsByDifficulty("HARD")
                    
                    val selected = mutableListOf<String>()
                    easyQuests.shuffled().firstOrNull()?.id?.let { selected.add(it) }
                    medQuests.shuffled().firstOrNull()?.id?.let { selected.add(it) }
                    hardQuests.shuffled().firstOrNull()?.id?.let { selected.add(it) }
                    
                    // Assign new ones if not already assigned
                    selected.forEach { qId ->
                        if (userQuests.none { it.quest_id == qId }) {
                            repository.assignQuestToUser(userId, qId)
                        }
                    }
                    prefs.edit().putString("last_refresh_$userId", today).apply()
                }

                _userQuests.value = repository.getUserQuests(userId).filter { 
                    it.updated_at.startsWith(today) || it.status != "CLAIMED" 
                }
            } catch (_: Exception) { }
        }
    }

    private val gardenRepository = com.example.releaf.data.repository.GardenRepository()
    private val authRepository = com.example.releaf.data.repository.AuthRepository()

    fun claimQuest(questId: String, userId: String) {
        viewModelScope.launch {
            val userQuest = repository.claimQuest(questId, userId)
            val quest = userQuest?.quest
            if (quest != null) {
                try {
                    val garden = gardenRepository.getGarden(userId)
                    if (garden != null) {
                        val newPoints = if (quest.reward_label == "Points") garden.current_points + quest.reward_count else garden.current_points
                        val newGems = if (quest.reward_label == "Gems") garden.current_gems + quest.reward_count else garden.current_gems
                        
                        gardenRepository.updateGarden(
                            userId,
                            com.example.releaf.data.remote.dto.GardenUpdateDto(
                                current_exp = garden.current_exp,
                                exp_target = garden.exp_target,
                                grow_uses_left = garden.grow_uses_left,
                                fertilize_uses_left = garden.fertilize_uses_left,
                                current_points = newPoints,
                                current_gems = newGems
                            )
                        )

                        // Also update global total points if it was points reward
                        if (quest.reward_label == "Points") {
                            val profile = authRepository.getProfile(userId)
                            if (profile != null) {
                                authRepository.updateTotalPoints(userId, profile.total_points + quest.reward_count)
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
            loadQuests(userId)
        }
    }

    fun updateQuestProgress(questId: String, userId: String, progress: Int, target: Int, status: String) {
        viewModelScope.launch {
            val newStatus = if (progress >= target) "CLAIMABLE" else status
            repository.updateUserQuest(questId, userId, UserQuestUpdateDto(progress, newStatus))
            loadQuests(userId)
        }
    }
}
