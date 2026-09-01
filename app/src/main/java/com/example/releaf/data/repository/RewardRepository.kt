package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.RewardTierDto
import com.example.releaf.data.remote.dto.UserRewardDto
import com.example.releaf.data.remote.dto.UserAchievementDto
import com.example.releaf.data.remote.dto.AchievementDto
import com.example.releaf.data.remote.dto.PlantSlotDto
import io.github.jan.supabase.postgrest.postgrest

class RewardRepository {
    private val client = SupabaseModule.client

    suspend fun getRewardTiers(): List<RewardTierDto> {
        return client.postgrest.from("reward_tiers").select().decodeList()
    }

    suspend fun getUserRewards(userId: String): List<UserRewardDto> {
        return client.postgrest.from("user_rewards")
            .select { filter { eq("user_id", userId) } }
            .decodeList()
    }

    suspend fun unlockTier(userId: String, tierId: String) {
        client.postgrest.from("user_rewards").insert(
            UserRewardDto(user_id = userId, tier_id = tierId)
        )
    }

    suspend fun getUserAchievements(userId: String): List<UserAchievementDto> {
        return client.postgrest.from("user_achievements")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList()
    }

    suspend fun getTotalAchievements(): Int {
        return client.postgrest.from("achievements")
            .select()
            .decodeList<AchievementDto>().size
    }

    suspend fun updateUserPoints(userId: String, points: Int) {
        client.postgrest.from("profiles").update(
            mapOf("total_points" to points)
        ) { filter { eq("id", userId) } }
    }

    suspend fun getGardenSlots(userId: String): List<PlantSlotDto> {
        return try {
            client.postgrest.from("plant_slots")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<PlantSlotDto>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun claimPlantReward(userId: String, slotIndex: Int, plantType: String? = null) {
        try {
            val data = mutableMapOf<String, Any>(
                "user_id" to userId,
                "slot_index" to slotIndex,
                "state" to "PLANTED"
            )
            plantType?.let { data["plant_type"] = it }
            client.postgrest.from("plant_slots").upsert(data) {
                onConflict = "user_id,slot_index"
            }
        } catch (e: Exception) {
            try {
                val updateData = mutableMapOf<String, Any>("state" to "PLANTED")
                plantType?.let { updateData["plant_type"] = it }
                client.postgrest.from("plant_slots")
                    .update(updateData) {
                        filter {
                            eq("user_id", userId)
                            eq("slot_index", slotIndex)
                        }
                    }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
