package com.example.releaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val title: String = "",
    val avatar_url: String = "",
    val total_points: Int = 0,
    val created_at: String = ""
)

@Serializable
data class ProfileUpdateDto(
    val name: String,
    val phone: String,
    val avatar_url: String
)
