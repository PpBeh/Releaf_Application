package com.example.releaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RewardTierDto(
    val id: String,
    val target_points: Int,
    val reward_description: String = "",
    val created_at: String = ""
)

@Serializable
data class UserRewardDto(
    val id: String = "",
    val user_id: String,
    val tier_id: String,
    val unlocked_at: String = ""
)

@Serializable
data class AchievementDto(
    val id: String,
    val label: String,
    val description: String = "",
    val icon_url: String = ""
)

@Serializable
data class UserAchievementDto(
    val id: String = "",
    val user_id: String,
    val achievement_id: String,
    val earned_at: String = "",
    val achievement: AchievementDto? = null
)
