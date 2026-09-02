package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.PlantSlotDto
import com.example.releaf.data.remote.dto.ProfileDto
import com.example.releaf.data.remote.dto.ReviewDto
import com.example.releaf.data.remote.dto.ReviewInsertDto
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
data class ReviewVoteDto(
    val id: String? = null,
    val review_id: String,
    val user_id: String,
    val vote_type: String,
    val created_at: String? = null
)

@Serializable
data class PoiStatsUpdateDto(
    val rating: Double,
    val cleanliness: String
)

class ReviewRepository {
    private val client = SupabaseModule.client

    suspend fun getReviews(poiId: String): List<ReviewDto> {
        return try {
            val reviews = client.postgrest.from("reviews")
                .select { filter { eq("poi_id", poiId) } }
                .decodeList<ReviewDto>()

            if (reviews.isEmpty()) return emptyList()

            val userIds = reviews.map { it.user_id }.distinct()
            val profiles = try {
                client.postgrest.from("profiles")
                    .select { filter { isIn("id", userIds) } }
                    .decodeList<ProfileDto>()
                    .associateBy { it.id }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
            reviews.map { review ->
                val userProfile = profiles[review.user_id]
                review.copy(
                    reviewer_name = userProfile?.name?.takeIf { it.isNotBlank() }
                        ?: review.reviewer_name.ifBlank { "User" },
                    reviewer_avatar_url = userProfile?.avatar_url ?: review.reviewer_avatar_url,
                    reviewer_frame = userProfile?.avatar_frame ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addReview(review: ReviewInsertDto): ReviewDto? {
        return try {
            client.postgrest.from("reviews").insert(review).decodeSingleOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateReview(reviewId: String, newText: String) {
        try {
            client.postgrest.from("reviews").update(
                mapOf("text" to newText)
            ) { filter { eq("id", reviewId) } }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteReview(reviewId: String) {
        try {
            client.postgrest.from("reviews").delete {
                filter { eq("id", reviewId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun reportReview(reviewId: String, userId: String) {
        try {
            client.postgrest.from("review_reports").insert(
                mapOf("review_id" to reviewId, "user_id" to userId)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserVotes(userId: String, reviewIds: List<String>): Map<String, String> {
        if (reviewIds.isEmpty()) return emptyMap()
        return try {
            val votes = client.postgrest.from("review_votes")
                .select { filter { eq("user_id", userId) } }
                .decodeList<ReviewVoteDto>()
            votes.filter { it.review_id in reviewIds }.associate { it.review_id to it.vote_type }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    suspend fun vote(reviewId: String, userId: String, voteType: String, review: ReviewDto): VoteResult {
        val existing = try {
            client.postgrest.from("review_votes")
                .select { filter { eq("review_id", reviewId); eq("user_id", userId) } }
                .decodeList<ReviewVoteDto>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

        if (existing.isNotEmpty()) {
            val oldVote = existing.first()
            val oldVoteId = oldVote.id
            if (oldVote.vote_type == voteType) {
                try {
                    if (oldVoteId != null) {
                        client.postgrest.from("review_votes").delete { filter { eq("id", oldVoteId) } }
                    } else {
                        client.postgrest.from("review_votes").delete { filter { eq("review_id", reviewId); eq("user_id", userId) } }
                    }
                } catch (_: Exception) {
                    try { client.postgrest.from("review_votes").delete { filter { eq("review_id", reviewId); eq("user_id", userId) } } } catch (_: Exception) {}
                }
                if (voteType == "LIKE") {
                    client.postgrest.from("reviews").update(
                        mapOf("like_count" to (review.like_count - 1).coerceAtLeast(0))
                    ) { filter { eq("id", reviewId) } }
                } else {
                    client.postgrest.from("reviews").update(
                        mapOf("dislike_count" to (review.dislike_count - 1).coerceAtLeast(0))
                    ) { filter { eq("id", reviewId) } }
                }
                return VoteResult.Unvoted
            } else {
                try {
                    if (oldVoteId != null) {
                        client.postgrest.from("review_votes").delete { filter { eq("id", oldVoteId) } }
                    } else {
                        client.postgrest.from("review_votes").delete { filter { eq("review_id", reviewId); eq("user_id", userId) } }
                    }
                } catch (_: Exception) {}
                client.postgrest.from("review_votes").insert(
                    ReviewVoteDto(review_id = reviewId, user_id = userId, vote_type = voteType)
                )
                if (voteType == "LIKE") {
                    client.postgrest.from("reviews").update(
                        mapOf(
                            "like_count" to review.like_count + 1,
                            "dislike_count" to (review.dislike_count - 1).coerceAtLeast(0)
                        )
                    ) { filter { eq("id", reviewId) } }
                } else {
                    client.postgrest.from("reviews").update(
                        mapOf(
                            "dislike_count" to review.dislike_count + 1,
                            "like_count" to (review.like_count - 1).coerceAtLeast(0)
                        )
                    ) { filter { eq("id", reviewId) } }
                }
                return VoteResult.Success
            }
        }

        client.postgrest.from("review_votes").insert(
            ReviewVoteDto(review_id = reviewId, user_id = userId, vote_type = voteType)
        )

        if (voteType == "LIKE") {
            client.postgrest.from("reviews").update(
                mapOf("like_count" to review.like_count + 1)
            ) { filter { eq("id", reviewId) } }
        } else {
            client.postgrest.from("reviews").update(
                mapOf("dislike_count" to review.dislike_count + 1)
            ) { filter { eq("id", reviewId) } }
        }

        return VoteResult.Success
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

    suspend fun updatePoiStats(poiId: String) {
        try {
            val reviews = getReviews(poiId)
            if (reviews.isEmpty()) return

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

            val cleanliness = when {
                avgRating >= 4.0 && cleanCount > dirtyCount -> "CLEAN"
                avgRating <= 2.0 || dirtyCount > cleanCount -> "DIRTY"
                else -> "AVERAGE"
            }

            val roundedRating = (avgRating * 10).toInt() / 10.0
            client.postgrest.from("pois").update(
                PoiStatsUpdateDto(rating = roundedRating, cleanliness = cleanliness)
            ) { filter { eq("id", poiId) } }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun analyzeReviewAndUpdatePoi(poiId: String, reviewText: String, starRating: Int) {
        val status = ReviewAnalyzer.analyze(reviewText, starRating) ?: return
        try {
            val reviews = getReviews(poiId)
            val latestTime = reviews.maxByOrNull { it.created_at }?.created_at ?: return
            client.postgrest.from("pois").update(
                mapOf(
                    "recent_status" to status,
                    "recent_status_time" to latestTime
                )
            ) { filter { eq("id", poiId) } }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getLatestAnalyzedStatus(poiId: String): Pair<String?, String?> {
        return try {
            val reviews = getReviews(poiId)
            if (reviews.isEmpty()) return Pair(null, null)

            val sorted = reviews.sortedByDescending { it.created_at }
            val recent = sorted.take(5)

            for (review in recent) {
                val status = ReviewAnalyzer.analyze(review.text, review.star_rating)
                if (status != null) {
                    return Pair(status, review.created_at)
                }
            }
            Pair(null, null)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    suspend fun getReviewerProfile(userId: String): ProfileDto? {
        return client.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull()
    }

}

sealed class VoteResult {
    data object Success : VoteResult()
    data object Unvoted : VoteResult()
    data object AlreadyVoted : VoteResult()
}