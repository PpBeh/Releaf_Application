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
                .select { filter { eq("user_id", userId); isIn("review_id", reviewIds) } }
                .decodeList<ReviewVoteDto>()
            votes.associate { it.review_id to it.vote_type }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    suspend fun vote(reviewId: String, userId: String, voteType: String, review: ReviewDto): VoteResult {
        // Read the freshest counts from the server before writing so concurrent
        // votes cannot overwrite each other with stale absolute values.
        var latest = review
        try {
            latest = client.postgrest.from("reviews")
                .select { filter { eq("id", reviewId) } }
                .decodeSingleOrNull() ?: review
        } catch (_: Exception) { }

        return try {
            val existing = client.postgrest.from("review_votes")
                .select { filter { eq("review_id", reviewId); eq("user_id", userId) } }
                .decodeList<ReviewVoteDto>()

            if (existing.isNotEmpty()) {
                val oldVote = existing.first()
                val oldVoteId = oldVote.id
                if (oldVote.vote_type == voteType) {
                    deleteVote(oldVoteId, reviewId, userId)
                    if (voteType == "LIKE") {
                        updateCounts(reviewId, likeDelta = -1, latestLike = latest.like_count)
                    } else {
                        updateCounts(reviewId, dislikeDelta = -1, latestDislike = latest.dislike_count)
                    }
                    return VoteResult.Unvoted
                } else {
                    deleteVote(oldVoteId, reviewId, userId)
                    client.postgrest.from("review_votes").insert(
                        ReviewVoteDto(review_id = reviewId, user_id = userId, vote_type = voteType)
                    )
                    if (voteType == "LIKE") {
                        updateCounts(
                            reviewId,
                            likeDelta = 1,
                            dislikeDelta = -1,
                            latestLike = latest.like_count,
                            latestDislike = latest.dislike_count
                        )
                    } else {
                        updateCounts(
                            reviewId,
                            likeDelta = -1,
                            dislikeDelta = 1,
                            latestLike = latest.like_count,
                            latestDislike = latest.dislike_count
                        )
                    }
                    return VoteResult.Success
                }
            }

            client.postgrest.from("review_votes").insert(
                ReviewVoteDto(review_id = reviewId, user_id = userId, vote_type = voteType)
            )
            if (voteType == "LIKE") {
                updateCounts(reviewId, likeDelta = 1, latestLike = latest.like_count)
            } else {
                updateCounts(reviewId, dislikeDelta = 1, latestDislike = latest.dislike_count)
            }
            VoteResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            VoteResult.AlreadyVoted
        }
    }

    private suspend fun deleteVote(voteId: String?, reviewId: String, userId: String) {
        try {
            if (voteId != null) {
                client.postgrest.from("review_votes").delete { filter { eq("id", voteId) } }
            } else {
                client.postgrest.from("review_votes").delete { filter { eq("review_id", reviewId); eq("user_id", userId) } }
            }
        } catch (_: Exception) {
            try { client.postgrest.from("review_votes").delete { filter { eq("review_id", reviewId); eq("user_id", userId) } } } catch (_: Exception) {}
        }
    }

    private suspend fun updateCounts(
        reviewId: String,
        likeDelta: Int = 0,
        dislikeDelta: Int = 0,
        latestLike: Int? = null,
        latestDislike: Int? = null
    ) {
        val update = mutableMapOf<String, Any>()
        if (likeDelta != 0) {
            val base = if (latestLike != null) latestLike else likeCount(reviewId)
            update["like_count"] = (base + likeDelta).coerceAtLeast(0)
        }
        if (dislikeDelta != 0) {
            val base = if (latestDislike != null) latestDislike else dislikeCount(reviewId)
            update["dislike_count"] = (base + dislikeDelta).coerceAtLeast(0)
        }
        if (update.isNotEmpty()) {
            client.postgrest.from("reviews").update(update) { filter { eq("id", reviewId) } }
        }
    }

    private suspend fun likeCount(reviewId: String): Int {
        return try {
            client.postgrest.from("reviews")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("like_count")) { filter { eq("id", reviewId) } }
                .decodeList<com.example.releaf.data.remote.dto.ReviewCountDto>().firstOrNull()?.like_count ?: 0
        } catch (_: Exception) { 0 }
    }

    private suspend fun dislikeCount(reviewId: String): Int {
        return try {
            client.postgrest.from("reviews")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("dislike_count")) { filter { eq("id", reviewId) } }
                .decodeList<com.example.releaf.data.remote.dto.ReviewCountDto>().firstOrNull()?.dislike_count ?: 0
        } catch (_: Exception) { 0 }
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

            val recent = reviews.sortedByDescending { it.created_at }.take(5)

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

    suspend fun countUserReviews(userId: String): Int {
        return try {
            client.postgrest.from("reviews")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("id")) { filter { eq("user_id", userId) } }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .size
        } catch (_: Exception) {
            0
        }
    }

}

sealed class VoteResult {
    data object Success : VoteResult()
    data object Unvoted : VoteResult()
    data object AlreadyVoted : VoteResult()
}