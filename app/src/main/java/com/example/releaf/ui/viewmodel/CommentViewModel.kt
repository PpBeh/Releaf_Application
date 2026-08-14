package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.ReviewDto
import com.example.releaf.data.remote.dto.ReviewInsertDto
import com.example.releaf.data.repository.AuthRepository
import com.example.releaf.data.repository.PoiRepository
import com.example.releaf.data.repository.ReviewRepository
import com.example.releaf.data.repository.VoteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentViewModel : ViewModel() {
    private val repository = ReviewRepository()
    private val poiRepository = PoiRepository()
    private val authRepository = AuthRepository()

    private val _reviews = MutableStateFlow<List<ReviewDto>>(emptyList())
    val reviews: StateFlow<List<ReviewDto>> = _reviews.asStateFlow()

    private val _userVotes = MutableStateFlow<Map<String, String>>(emptyMap())
    val userVotes: StateFlow<Map<String, String>> = _userVotes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isVoting = MutableStateFlow(false)
    val isVoting: StateFlow<Boolean> = _isVoting.asStateFlow()

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
            } catch (_: Exception) {
                _reviews.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addReview(poiId: String, userId: String, starRating: Int, text: String, userLat: Double, userLng: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existingReviews = try {
                    repository.getReviews(poiId)
                } catch (_: Exception) {
                    emptyList()
                }
                if (existingReviews.any { it.user_id == userId }) {
                    _errorMessage.value = "You have already commented on this toilet. Only one comment per user is allowed."
                    _isLoading.value = false
                    return@launch
                }

                val poi = poiRepository.getPoi(poiId)
                if (poi != null && userLat != 0.0 && userLng != 0.0) {
                    val distance = haversine(userLat, userLng, poi.latitude, poi.longitude)
                    if (distance > 300.0) {
                        _errorMessage.value = "You must be at the location to leave a comment (you are ${distance.toInt()}m away)."
                        _isLoading.value = false
                        return@launch
                    }
                }
                val reviewerName = try {
                    authRepository.getProfile(userId)?.name?.ifBlank { "User" } ?: "User"
                } catch (_: Exception) {
                    "User"
                }
                repository.addReview(
                    ReviewInsertDto(
                        poi_id = poiId,
                        user_id = userId,
                        star_rating = starRating,
                        text = text,
                        reviewer_name = reviewerName
                    )
                )
                repository.analyzeReviewAndUpdatePoi(poiId, text, starRating)
                kotlinx.coroutines.delay(1500)
                _reviews.value = repository.getReviews(poiId)
                repository.updatePoiStats(poiId)
                _errorMessage.value = null
            } catch (_: Exception) { }
            _isLoading.value = false
        }
    }

    fun updateReview(reviewId: String, newText: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.updateReview(reviewId, newText)
                kotlinx.coroutines.delay(1000)
                _reviews.value = repository.getReviews(currentPoiId)
                repository.updatePoiStats(currentPoiId)
            } catch (_: Exception) { }
            _isProcessing.value = false
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.deleteReview(reviewId)
                kotlinx.coroutines.delay(1000)
                _reviews.value = repository.getReviews(currentPoiId)
                repository.updatePoiStats(currentPoiId)
            } catch (_: Exception) { }
            _isProcessing.value = false
        }
    }

    fun reportReview(reviewId: String) {
        viewModelScope.launch {
            try {
                repository.reportReview(reviewId, currentUserId)
                _errorMessage.value = "Comment reported. Thank you."
            } catch (_: Exception) { }
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun toggleLike(review: ReviewDto) {
        if (_isVoting.value) return
        viewModelScope.launch {
            _isVoting.value = true
            try {
                val result = repository.vote(review.id, currentUserId, "LIKE", review)
                val votes = _userVotes.value.toMutableMap()
                when (result) {
                    is VoteResult.Success -> votes[review.id] = "LIKE"
                    is VoteResult.Unvoted -> votes.remove(review.id)
                    is VoteResult.AlreadyVoted -> { }
                }
                _userVotes.value = votes
                _reviews.value = repository.getReviews(currentPoiId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Like failed: ${e.message}"
            } finally {
                _isVoting.value = false
            }
        }
    }

    fun toggleDislike(review: ReviewDto) {
        if (_isVoting.value) return
        viewModelScope.launch {
            _isVoting.value = true
            try {
                val result = repository.vote(review.id, currentUserId, "DISLIKE", review)
                val votes = _userVotes.value.toMutableMap()
                when (result) {
                    is VoteResult.Success -> votes[review.id] = "DISLIKE"
                    is VoteResult.Unvoted -> votes.remove(review.id)
                    is VoteResult.AlreadyVoted -> { }
                }
                _userVotes.value = votes
                _reviews.value = repository.getReviews(currentPoiId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Dislike failed: ${e.message}"
            } finally {
                _isVoting.value = false
            }
        }
    }
}
