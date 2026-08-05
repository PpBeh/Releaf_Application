package com.example.releaf.model

data class Review(
    val id: String,
    val poiId: String,
    val reviewerUserId: String,
    val starRating: Int,
    val text: String,
    val timeAgoLabel: String,
    val likeCount: Int,
    val dislikeCount: Int
)