package com.example.releaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GardenDto(
    val id: String = "",
    val user_id: String,
    val current_exp: Int = 0,
    val exp_target: Int = 1000,
    val grow_uses_left: Int = 1,
    val grow_uses_max: Int = 1,
    val fertilize_uses_left: Int = 1,
    val fertilize_uses_max: Int = 1,
    val current_points: Int = 0,
    val points_target: Int = 100,
    val updated_at: String = ""
)

@Serializable
data class PlantSlotDto(
    val id: String = "",
    val user_id: String,
    val slot_index: Int,
    val state: String = "EMPTY_POT",
    val plant_type: String? = null,
    val planted_at: String? = null
)

@Serializable
data class GardenUpdateDto(
    val current_exp: Int,
    val exp_target: Int,
    val grow_uses_left: Int,
    val fertilize_uses_left: Int,
    val current_points: Int
)
