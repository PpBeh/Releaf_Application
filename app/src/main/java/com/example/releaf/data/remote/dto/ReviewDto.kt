package com.example.releaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: String,
    val poi_id: String,
    val user_id: String,
    val star_rating: Int,
    val text: String,
    val like_count: Int = 0,
    val dislike_count: Int = 0,
    val reviewer_name: String = "",
    val reviewer_avatar_url: String = "",
    val photo_url: String? = null,
    val created_at: String = ""
)

@Serializable
data class ReviewInsertDto(
    val poi_id: String,
    val user_id: String,
    val star_rating: Int,
    val text: String,
    val reviewer_name: String = "",
    val photo_url: String? = null
)
