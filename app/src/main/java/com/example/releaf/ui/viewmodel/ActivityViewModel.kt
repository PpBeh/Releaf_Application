package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.UserQuestDto
import com.example.releaf.data.remote.dto.UserQuestUpdateDto
import com.example.releaf.data.repository.QuestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivityViewModel : ViewModel() {
    private val repository = QuestRepository()

    private val _userQuests = MutableStateFlow<List<UserQuestDto>>(emptyList())
    val userQuests: StateFlow<List<UserQuestDto>> = _userQuests.asStateFlow()

    fun loadQuests(userId: String) {
        viewModelScope.launch {
            try {
                val allQuests = repository.getAllQuests()
                val userQuests = repository.getUserQuests(userId)

                val assigned = allQuests.mapNotNull { quest ->
                    val existing = userQuests.find { it.quest_id == quest.id }
                    if (existing != null) existing
                    else {
                        repository.assignQuestToUser(userId, quest.id)
                        UserQuestDto(
                            user_id = userId,
                            quest_id = quest.id,
                            status = "IN_PROGRESS",
                            quest = quest
                        )
                    }
                }
                _userQuests.value = assigned
            } catch (_: Exception) { }
        }
    }

    fun claimQuest(questId: String, userId: String) {
        viewModelScope.launch {
            repository.claimQuest(questId, userId)
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
