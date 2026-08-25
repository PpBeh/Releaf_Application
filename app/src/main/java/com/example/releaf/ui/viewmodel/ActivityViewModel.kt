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
import kotlin.math.max

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuestRepository()
    private val activityPrefs = application.getSharedPreferences("activity_prefs", Context.MODE_PRIVATE)

    private val gardenPrefs = application.getSharedPreferences("garden_prefs", Context.MODE_PRIVATE)

    private val _userQuests = MutableStateFlow<List<UserQuestDto>>(emptyList())
    val userQuests: StateFlow<List<UserQuestDto>> = _userQuests.asStateFlow()

    fun loadQuests(userId: String) {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                try {
                    repository.incrementQuestsByType(userId, "CHECK_IN")
                } catch (e: Exception) {
                    android.util.Log.e("ActivityViewModel", "Check-in failed", e)
                }

                val allAvailableQuests = repository.getQuestsByDifficulty("EASY") +
                        repository.getQuestsByDifficulty("MEDIUM") +
                        repository.getQuestsByDifficulty("HARD")
                val currentUserQuests = repository.getUserQuests(userId)

                currentUserQuests.forEach { uq ->
                    val lastUpdate = uq.updated_at ?: ""
                    if (!lastUpdate.startsWith(today)) {
                        try {
                            repository.updateUserQuest(
                                uq.quest_id,
                                userId,
                                UserQuestUpdateDto(progress_current = 0, status = "IN_PROGRESS")
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("ActivityViewModel", "Failed to reset quest", e)
                        }
                    }
                }

                allAvailableQuests.forEach { quest ->
                    if (currentUserQuests.none { it.quest_id == quest.id }) {
                        try {
                            repository.assignQuestToUser(userId, quest.id)
                        } catch (e: Exception) {
                            android.util.Log.e("ActivityViewModel", "Failed to assign new quest", e)
                        }
                    }
                }

                val updatedQuests = repository.getUserQuests(userId)

                val statusOrder = mapOf("CLAIMABLE" to 0, "IN_PROGRESS" to 1, "CLAIMED" to 2)
                val difficultyOrder = mapOf("EASY" to 0, "MEDIUM" to 1, "HARD" to 2)

                _userQuests.value = updatedQuests
                    .filter { (it.updated_at ?: "").startsWith(today) || it.status != "CLAIMED" }
                    .sortedWith(compareBy(
                        { statusOrder[it.status] ?: 99 },
                        { difficultyOrder[it.quest?.difficulty] ?: 99 }
                    ))

            } catch (e: Exception) {
                android.util.Log.e("ActivityViewModel", "Error loading quests completely", e)
            }
        }
    }

    private val gardenRepository = com.example.releaf.data.repository.GardenRepository()
    private val authRepository = com.example.releaf.data.repository.AuthRepository()

    fun claimQuest(questId: String, userId: String) {
        viewModelScope.launch {
            _userQuests.value = _userQuests.value.map {
                if (it.quest_id == questId) it.copy(status = "CLAIMED") else it
            }

            try {
                val userQuest = repository.claimQuest(questId, userId)
                val quest = repository.getQuest(questId) ?: userQuest?.quest

                if (quest != null) {
                    val garden = gardenRepository.getGarden(userId)

                    val localExp = gardenPrefs.getInt("tree_exp_${userId}", 0)
                    val remoteExp = garden?.current_exp ?: 0
                    val actualExp = max(localExp, remoteExp)

                    val newExp = actualExp + quest.reward_count
                    val newPoints = (garden?.current_points ?: 0) + quest.reward_count

                    gardenPrefs.edit().putInt("tree_exp_${userId}", newExp).apply()

                    gardenRepository.upsertGardenExp(
                        userId = userId,
                        newExp = newExp,
                        newPoints = newPoints,
                        newGems = garden?.current_gems ?: 0,
                        expTarget = garden?.exp_target ?: 2000,
                        waterUsesLeft = garden?.grow_uses_left ?: 1,
                        fertilizeUsesLeft = garden?.fertilize_uses_left ?: 1
                    )

                    val profile = authRepository.getProfile(userId)
                    if (profile != null) {
                        authRepository.updateTotalPoints(userId, profile.total_points + quest.reward_count)
                    }

                    com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivityViewModel", "Error claiming quest", e)
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