package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.ReviewDto
import com.example.releaf.data.remote.dto.ReviewInsertDto
import com.example.releaf.data.repository.ReviewRepository
import com.example.releaf.data.repository.VoteResult
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentViewModel : ViewModel() {
    private val repository = ReviewRepository()

    private val _reviews = MutableStateFlow<List<ReviewDto>>(emptyList())
    val reviews: StateFlow<List<ReviewDto>> = _reviews.asStateFlow()

    private val _userVotes = MutableStateFlow<Map<String, String>>(emptyMap())
    val userVotes: StateFlow<Map<String, String>> = _userVotes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isVoting = MutableStateFlow(false)
    val isVoting: StateFlow<Boolean> = _isVoting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentPoiId = ""
    private var currentUserId = ""

    fun loadReviews(poiId: String, userId: String) {
        currentPoiId = poiId
        currentUserId = userId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val reviews = repository.getReviews(poiId)
                _reviews.value = reviews
                val votes = repository.getUserVotes(userId, reviews.map { it.id })
                _userVotes.value = votes
            } catch (e: Exception) {
                e.printStackTrace()
                _reviews.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addReview(
        poiId: String,
        userId: String,
        starRating: Int,
        text: String,
        userLat: Double,
        userLng: Double,
        photoUri: android.net.Uri? = null,
        context: android.content.Context? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            var success = false
            try {
                val existingReviews = try {
                    repository.getReviews(poiId)
                } catch (e: Exception) {
                    emptyList()
                }
                if (existingReviews.count { it.user_id == userId } >= 3) {
                    _errorMessage.value =
                        "You have reached the limit of 3 comments for this location."
                    _isProcessing.value = false
                    return@launch
                }

                val reviewerName = try {
                    com.example.releaf.data.repository.AuthRepository()
                        .getProfile(userId)?.name?.ifBlank { "User" } ?: "User"
                } catch (_: Exception) {
                    "User"
                }
                var uploadedPhotoUrl: String? = null
                if (photoUri != null && context != null) {
                    try {
                        val bytes = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                context.contentResolver.openInputStream(photoUri)
                                    ?.use { it.readBytes() }
                            } catch (_: Exception) {
                                try {
                                    java.io.File(photoUri.path ?: "").readBytes()
                                        .takeIf { it.isNotEmpty() }
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }
                        if (bytes != null && bytes.isNotEmpty()) {
                            val fileName = "review_${userId}_${System.currentTimeMillis()}.jpg"
                            com.example.releaf.data.remote.SupabaseModule.client.storage.from("poi-photos")
                                .upload(
                                    path = fileName,
                                    data = bytes
                                ) { upsert = true }
                            uploadedPhotoUrl =
                                com.example.releaf.data.remote.SupabaseModule.client.storage.from("poi-photos")
                                    .publicUrl(fileName)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.util.Log.e("CommentVM", "photo upload failed", e)
                    }
                }

                val posted = repository.addReview(
                    ReviewInsertDto(
                        poi_id = poiId,
                        user_id = userId,
                        star_rating = starRating,
                        text = text,
                        reviewer_name = reviewerName,
                        photo_url = uploadedPhotoUrl
                    )
                )
                if (!posted) {
                    _errorMessage.value =
                        "Could not post your comment. Check your connection and try again."
                    _isProcessing.value = false
                    return@launch
                }

                // The comment is on the server. Everything below is background
                // housekeeping - a failure here must NOT report the post as failed
                // (that caused phantom errors and duplicate posts on retry).
                success = true
                _errorMessage.value = null
                try {
                    com.example.releaf.data.repository.QuestRepository()
                        .incrementQuestsByType(userId, "REVIEW")
                } catch (_: Exception) {
                }
                try {
                    _reviews.value = repository.getReviews(poiId)
                } catch (_: Exception) {
                }
                try {
                    repository.updatePoiStats(poiId)
                } catch (_: Exception) {
                }
                try {
                    repository.analyzeReviewAndUpdatePoi(poiId, text, starRating)
                } catch (_: Exception) {
                }
                com.example.releaf.data.remote.SupabaseModule.triggerRefresh()
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("CommentViewModel", "Error adding review", e)
                _errorMessage.value =
                    "Could not post your comment. Check your connection and try again."
            }
            _isProcessing.value = false
            onAddReviewResult?.invoke(success)
        }
    }

    private var onAddReviewResult: ((Boolean) -> Unit)? = null

    fun registerAddReviewCallback(callback: ((Boolean) -> Unit)?) {
        onAddReviewResult = callback
    }

    fun updateReview(reviewId: String, newText: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.updateReview(reviewId, newText)
                _reviews.value = repository.getReviews(currentPoiId)
                repository.updatePoiStats(currentPoiId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isProcessing.value = false
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.deleteReview(reviewId)
                _reviews.value = repository.getReviews(currentPoiId)
                repository.updatePoiStats(currentPoiId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isProcessing.value = false
        }
    }

    fun reportReview(reviewId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.reportReview(reviewId, currentUserId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isProcessing.value = false
        }
    }

    fun toggleLike(review: ReviewDto) {
        viewModelScope.launch {
            _isVoting.value = true
            try {
                val result = repository.vote(review.id, currentUserId, "LIKE", review)
                val votes = _userVotes.value.toMutableMap()
                when (result) {
                    is VoteResult.Success -> votes[review.id] = "LIKE"
                    is VoteResult.Unvoted -> votes.remove(review.id)
                    is VoteResult.AlreadyVoted -> {}
                }
                _userVotes.value = votes
                _reviews.value = repository.getReviews(currentPoiId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isVoting.value = false
        }
    }

    fun toggleDislike(review: ReviewDto) {
        viewModelScope.launch {
            _isVoting.value = true
            try {
                val result = repository.vote(review.id, currentUserId, "DISLIKE", review)
                val votes = _userVotes.value.toMutableMap()
                when (result) {
                    is VoteResult.Success -> votes[review.id] = "DISLIKE"
                    is VoteResult.Unvoted -> votes.remove(review.id)
                    is VoteResult.AlreadyVoted -> {}
                }
                _userVotes.value = votes
                _reviews.value = repository.getReviews(currentPoiId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isVoting.value = false
        }
    }
}