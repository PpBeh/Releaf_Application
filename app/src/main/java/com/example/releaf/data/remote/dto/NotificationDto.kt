package com.example.releaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val user_id: String? = null,
    val type: String = "LIKE",
    val title: String = "",
    val body: String = "",
    val is_read: Boolean = false,
    val created_at: String = ""
)
