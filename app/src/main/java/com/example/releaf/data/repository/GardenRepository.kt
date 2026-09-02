package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.GardenDto
import com.example.releaf.data.remote.dto.GardenUpdateDto
import com.example.releaf.data.remote.dto.PlantSlotDto
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class GardenUpsertDto(
    val user_id: String,
    val current_exp: Int,
    val exp_target: Int,
    val grow_uses_left: Int,
    val fertilize_uses_left: Int,
    val current_points: Int,
    val current_gems: Int
)

class GardenRepository {
    private val client = SupabaseModule.client

    suspend fun getGarden(userId: String): GardenDto? {
        return client.postgrest.from("gardens")
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull()
    }

    suspend fun getPlantSlots(userId: String): List<PlantSlotDto> {
        return client.postgrest.from("plant_slots")
            .select { filter { eq("user_id", userId) } }
            .decodeList()
    }

    suspend fun upsertGarden(garden: GardenDto) {
        client.postgrest.from("gardens").upsert(garden) {
            onConflict = "user_id"
        }
    }

    suspend fun upsertPlantSlot(slot: PlantSlotDto) {
        client.postgrest.from("plant_slots").upsert(slot) {
            onConflict = "user_id,slot_index"
        }
    }

    suspend fun upsertPlantSlot(userId: String, slotIndex: Int, state: String, plantType: String? = null) {
        val data = mutableMapOf<String, Any>(
            "user_id" to userId,
            "slot_index" to slotIndex,
            "state" to state
        )
        plantType?.let { data["plant_type"] = it }
        client.postgrest.from("plant_slots").upsert(data) {
            onConflict = "user_id,slot_index"
        }
    }

    suspend fun deleteGarden(userId: String) {
        client.postgrest.from("gardens").delete { filter { eq("user_id", userId) } }
    }

    suspend fun deletePlantSlot(userId: String, slotIndex: Int) {
        client.postgrest.from("plant_slots").delete {
            filter {
                eq("user_id", userId)
                eq("slot_index", slotIndex)
            }
        }
    }

    suspend fun deletePlantSlotById(slotId: String) {
        if (slotId.isBlank()) return
        client.postgrest.from("plant_slots").delete { filter { eq("id", slotId) } }
    }

    suspend fun updateGarden(userId: String, update: GardenUpdateDto) {
        client.postgrest.from("gardens")
            .update(update) { filter { eq("user_id", userId) } }
    }

    suspend fun updatePlantSlot(slotId: String, state: String, plantType: String? = null) {
        val update = mutableMapOf<String, Any>("state" to state)
        plantType?.let { update["plant_type"] = it }
        client.postgrest.from("plant_slots")
            .update(update) { filter { eq("id", slotId) } }
    }

    suspend fun upsertGardenExp(
        userId: String,
        newExp: Int,
        newPoints: Int,
        newGems: Int,
        expTarget: Int,
        waterUsesLeft: Int,
        fertilizeUsesLeft: Int
    ) {
        val upsertData = GardenUpsertDto(
            user_id = userId,
            current_exp = newExp,
            exp_target = expTarget,
            grow_uses_left = waterUsesLeft,
            fertilize_uses_left = fertilizeUsesLeft,
            current_points = newPoints.coerceAtLeast(0),
            current_gems = newGems.coerceAtLeast(0)
        )

        client.postgrest.from("gardens").upsert(upsertData) {
            onConflict = "user_id"
        }
    }

    // Repairs legacy negative balances (caused by the old frame-purchase bug) to 0.
    suspend fun healNegativeBalance(userId: String, garden: GardenDto): GardenDto {
        if (garden.current_points >= 0 && garden.current_gems >= 0) return garden
        val healed = garden.copy(
            current_points = garden.current_points.coerceAtLeast(0),
            current_gems = garden.current_gems.coerceAtLeast(0)
        )
        try {
            updateGarden(
                userId,
                GardenUpdateDto(
                    current_exp = healed.current_exp,
                    exp_target = healed.exp_target,
                    grow_uses_left = healed.grow_uses_left,
                    fertilize_uses_left = healed.fertilize_uses_left,
                    current_points = healed.current_points,
                    current_gems = healed.current_gems
                )
            )
        } catch (_: Exception) { }
        return healed
    }
}