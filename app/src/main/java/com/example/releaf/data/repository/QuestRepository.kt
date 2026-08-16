package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.QuestDto
import com.example.releaf.data.remote.dto.UserQuestDto
import com.example.releaf.data.remote.dto.UserQuestUpdateDto
import io.github.jan.supabase.postgrest.postgrest

class QuestRepository {
    private val client = SupabaseModule.client

    suspend fun getAllQuests(): List<QuestDto> {
        return client.postgrest.from("quests").select().decodeList()
    }

    suspend fun getQuest(questId: String): QuestDto? {
        return try {
            client.postgrest.from("quests")
                .select { filter { eq("id", questId) } }
                .decodeSingleOrNull()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getQuestsByDifficulty(difficulty: String): List<QuestDto> {
        return client.postgrest.from("quests")
            .select { filter { eq("difficulty", difficulty) } }
            .decodeList()
    }

    suspend fun getUserQuests(userId: String): List<UserQuestDto> {
        return client.postgrest.from("user_quests")
            .select(io.github.jan.supabase.postgrest.query.Columns.raw("*, quest:quests(*)")) {
                filter { eq("user_id", userId) }
            }
            .decodeList()
    }

    suspend fun updateUserQuest(questId: String, userId: String, update: UserQuestUpdateDto) {
        client.postgrest.from("user_quests")
            .update(update) {
                filter { eq("quest_id", questId); eq("user_id", userId) }
            }
    }

    suspend fun claimQuest(questId: String, userId: String): UserQuestDto? {
        updateUserQuest(questId, userId, UserQuestUpdateDto(progress_current = 0, status = "CLAIMED"))
        return getUserQuests(userId).find { it.quest_id == questId }
    }

    suspend fun assignQuestToUser(userId: String, questId: String) {
        client.postgrest.from("user_quests").insert(
            UserQuestDto(user_id = userId, quest_id = questId, status = "IN_PROGRESS")
        )
    }

    suspend fun incrementQuestsByType(userId: String, questType: String) {
        try {
            val quests = client.postgrest.from("quests")
                .select { filter { eq("quest_type", questType) } }
                .decodeList<QuestDto>()

            val userQuests = try {
                getUserQuests(userId)
            } catch (_: Exception) {
                emptyList()
            }

            for (quest in quests) {
                val userQuest = userQuests.find { it.quest_id == quest.id } ?: continue
                if (userQuest.status == "CLAIMED" || userQuest.status == "CLAIMABLE") continue

                val newProgress = userQuest.progress_current + 1
                val newStatus = if (newProgress >= quest.progress_target) "CLAIMABLE" else "IN_PROGRESS"
                try {
                    updateUserQuest(quest.id, userId, UserQuestUpdateDto(newProgress, newStatus))
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }
}
