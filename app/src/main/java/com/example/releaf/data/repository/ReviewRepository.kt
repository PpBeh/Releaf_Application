package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.ReviewDto
import com.example.releaf.data.remote.dto.ReviewInsertDto
import com.example.releaf.data.remote.dto.ProfileDto
import io.github.jan.supabase.postgrest.postgrest

class ReviewRepository {
    private val client = SupabaseModule.client

    suspend fun getReviews(poiId: String): List<ReviewDto> {
        return client.postgrest.from("reviews")
            .select { filter { eq("poi_id", poiId) } }
            .decodeList()
    }

    suspend fun addReview(review: ReviewInsertDto): ReviewDto? {
        return client.postgrest.from("reviews").insert(review).decodeSingleOrNull()
    }

    suspend fun likeReview(reviewId: String, currentLikes: Int) {
        client.postgrest.from("reviews").update(
            mapOf("like_count" to currentLikes + 1)
        ) { filter { eq("id", reviewId) } }
    }

    suspend fun dislikeReview(reviewId: String, currentDislikes: Int) {
        client.postgrest.from("reviews").update(
            mapOf("dislike_count" to currentDislikes + 1)
        ) { filter { eq("id", reviewId) } }
    }

    suspend fun getCleanlinessScore(poiId: String): String {
        val reviews = getReviews(poiId)
        if (reviews.isEmpty()) return "AVERAGE"

        val avgRating = reviews.map { it.star_rating }.average()
        val dirtyCount = reviews.count {
            it.text.contains("dirty", ignoreCase = true) ||
            it.text.contains("disgusting", ignoreCase = true) ||
            it.text.contains("filthy", ignoreCase = true)
        }
        val cleanCount = reviews.count {
            it.text.contains("clean", ignoreCase = true) ||
            it.text.contains("spotless", ignoreCase = true) ||
            it.text.contains("tidy", ignoreCase = true)
        }

        return when {
            avgRating >= 4.0 && cleanCount > dirtyCount -> "CLEAN"
            avgRating <= 2.0 || dirtyCount > cleanCount -> "DIRTY"
            else -> "AVERAGE"
        }
    }

    suspend fun updatePoiCleanliness(poiId: String) {
        val score = getCleanlinessScore(poiId)
        client.postgrest.from("pois").update(
            mapOf("cleanliness" to score)
        ) { filter { eq("id", poiId) } }
    }

    suspend fun getReviewerProfile(userId: String): ProfileDto? {
        return client.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull()
    }
}
