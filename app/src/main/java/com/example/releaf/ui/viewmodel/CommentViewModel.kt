package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.ReviewDto
import com.example.releaf.data.remote.dto.ReviewInsertDto
import com.example.releaf.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentViewModel : ViewModel() {
    private val repository = ReviewRepository()

    private val _reviews = MutableStateFlow<List<ReviewDto>>(emptyList())
    val reviews: StateFlow<List<ReviewDto>> = _reviews.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadReviews(poiId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _reviews.value = repository.getReviews(poiId)
            _isLoading.value = false
        }
    }

    fun addReview(poiId: String, userId: String, starRating: Int, text: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.addReview(
                ReviewInsertDto(poi_id = poiId, user_id = userId, star_rating = starRating, text = text)
            )
            _reviews.value = repository.getReviews(poiId)
            repository.updatePoiCleanliness(poiId)
            _isLoading.value = false
        }
    }

    fun likeReview(reviewId: String, currentLikes: Int) {
        viewModelScope.launch {
            repository.likeReview(reviewId, currentLikes)
            val poiId = _reviews.value.firstOrNull { it.id == reviewId }?.poi_id ?: return@launch
            _reviews.value = repository.getReviews(poiId)
        }
    }

    fun dislikeReview(reviewId: String, currentDislikes: Int) {
        viewModelScope.launch {
            repository.dislikeReview(reviewId, currentDislikes)
            val poiId = _reviews.value.firstOrNull { it.id == reviewId }?.poi_id ?: return@launch
            _reviews.value = repository.getReviews(poiId)
        }
    }
}
