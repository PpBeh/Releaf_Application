package com.example.releaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class QuestDto(
    val id: String,
    val title: String,
    val description: String,
    val reward_label: String,
    val reward_count: Int,
    val progress_target: Int,
    val difficulty: String = "EASY",
    val quest_type: String = "",
    val created_at: String = ""
)

@Serializable
data class UserQuestDto(
    val id: String = "",
    val user_id: String,
    val quest_id: String,
    val progress_current: Int = 0,
    val status: String = "IN_PROGRESS",
    val quest: QuestDto? = null,
    val updated_at: String = ""
)

@Serializable
data class UserQuestUpdateDto(
    val progress_current: Int,
    val status: String
)
