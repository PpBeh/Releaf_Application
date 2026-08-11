package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.GardenDto
import com.example.releaf.data.remote.dto.GardenUpdateDto
import com.example.releaf.data.remote.dto.PlantSlotDto
import io.github.jan.supabase.postgrest.postgrest

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
}
