package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.PoiDto
import com.example.releaf.data.remote.dto.PoiInsertDto
import com.example.releaf.data.remote.dto.PoiPhotoDto
import com.example.releaf.data.repository.PoiRepository
import com.example.releaf.data.repository.ReviewRepository
import com.example.releaf.data.repository.ReportResult
import com.example.releaf.data.repository.VerifyResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private val repository = PoiRepository()
    private val reviewRepository = ReviewRepository()

    private val _pois = MutableStateFlow<List<PoiDto>>(emptyList())
    val pois: StateFlow<List<PoiDto>> = _pois.asStateFlow()

    private val _selectedPoi = MutableStateFlow<PoiDto?>(null)
    val selectedPoi: StateFlow<PoiDto?> = _selectedPoi.asStateFlow()

    private val _filteredPois = MutableStateFlow<List<PoiDto>>(emptyList())
    val filteredPois: StateFlow<List<PoiDto>> = _filteredPois.asStateFlow()

    private val _photos = MutableStateFlow<List<PoiPhotoDto>>(emptyList())
    val photos: StateFlow<List<PoiPhotoDto>> = _photos.asStateFlow()

    private val _reviewCount = MutableStateFlow(0)
    val reviewCount: StateFlow<Int> = _reviewCount.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _enabledCategories = MutableStateFlow(setOf("TOILET", "TRASH_CAN"))
    private val _enabledCleanliness = MutableStateFlow(setOf("CLEAN", "AVERAGE", "DIRTY"))
    private val _excludedPaid = MutableStateFlow<Boolean?>(null)
    private val _showUnverified = MutableStateFlow(true)

    val enabledCategories: StateFlow<Set<String>> = _enabledCategories.asStateFlow()
    val enabledCleanliness: StateFlow<Set<String>> = _enabledCleanliness.asStateFlow()
    val excludedPaid: StateFlow<Boolean?> = _excludedPaid.asStateFlow()
    val showUnverified: StateFlow<Boolean> = _showUnverified.asStateFlow()

    private val _actionResult = MutableStateFlow<PoiActionResult?>(null)
    val actionResult: StateFlow<PoiActionResult?> = _actionResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _analyzedStatus = MutableStateFlow<String?>(null)
    val analyzedStatus: StateFlow<String?> = _analyzedStatus.asStateFlow()

    private val _analyzedStatusTime = MutableStateFlow<String?>(null)
    val analyzedStatusTime: StateFlow<String?> = _analyzedStatusTime.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PoiDto>>(emptyList())
    val searchResults: StateFlow<List<PoiDto>> = _searchResults.asStateFlow()

    init {
    }

    fun loadPois() {
        viewModelScope.launch {
            try {
                val result = repository.getAllPois()
                _pois.value = result
                applyFilters()
            } catch (_: Exception) { }
        }
    }

    fun selectPoi(poi: PoiDto) {
        _selectedPoi.value = poi
        viewModelScope.launch {
            try {
                _photos.value = repository.getPoiPhotos(poi.id)
                _reviewCount.value = repository.getReviewCount(poi.id)
                _isFavorite.value = repository.isFavorite(poi.id, currentUserId)
                val statusPair = reviewRepository.getLatestAnalyzedStatus(poi.id)
                _analyzedStatus.value = statusPair.first
                _analyzedStatusTime.value = statusPair.second
            } catch (_: Exception) { }
        }
    }

    private var currentUserId = ""

    fun setCurrentUserId(userId: String) {
        currentUserId = userId
    }

    fun toggleFavorite(poiId: String) {
        viewModelScope.launch {
            try {
                _isFavorite.value = repository.toggleFavorite(poiId, currentUserId)
            } catch (_: Exception) { }
        }
    }

    fun uploadPhoto(poiId: String, uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val success = repository.uploadPoiPhoto(poiId, currentUserId, uri, context)
                if (success) {
                    _photos.value = repository.getPoiPhotos(poiId)
                    _actionResult.value = PoiActionResult.Message("photo_uploaded")
                } else {
                    _actionResult.value = PoiActionResult.Message("photo_failed")
                }
            } catch (_: Exception) {
                _actionResult.value = PoiActionResult.Message("photo_failed")
            }
            _isProcessing.value = false
        }
    }

    fun clearSelection() {
        _selectedPoi.value = null
        _photos.value = emptyList()
        _reviewCount.value = 0
        _isFavorite.value = false
        _analyzedStatus.value = null
        _analyzedStatusTime.value = null
    }

    fun toggleCategory(category: String) {
        val current = _enabledCategories.value
        _enabledCategories.value = if (category in current) current - category else current + category
        applyFilters()
    }

    fun resetCategories() {
        _enabledCategories.value = setOf("TOILET", "TRASH_CAN")
        applyFilters()
    }

    fun toggleCleanliness(cleanliness: String) {
        val current = _enabledCleanliness.value
        _enabledCleanliness.value = if (cleanliness in current) current - cleanliness else current + cleanliness
        applyFilters()
    }

    fun resetCleanliness() {
        _enabledCleanliness.value = setOf("CLEAN", "AVERAGE", "DIRTY")
        applyFilters()
    }

    fun togglePaid(paid: Boolean?) {
        _excludedPaid.value = paid
        applyFilters()
    }

    fun toggleUnverified() {
        _showUnverified.value = !_showUnverified.value
        applyFilters()
    }

    private fun applyFilters() {
        var result = _pois.value
        result = result.filter { it.category in _enabledCategories.value }
        result = result.filter { it.cleanliness in _enabledCleanliness.value }
        _excludedPaid.value?.let { excluded ->
            result = result.filter { it.is_paid != excluded }
        }
        if (!_showUnverified.value) {
            result = result.filter { it.is_verified }
        }
        _filteredPois.value = result
    }

    fun createPoi(name: String, category: String, latitude: Double, longitude: Double, isPaid: Boolean, userId: String, description: String = "") {
        viewModelScope.launch {
            val tooClose = _pois.value.any {
                haversine(latitude, longitude, it.latitude, it.longitude) < 5.0
            }
            if (tooClose) {
                _actionResult.value = PoiActionResult.Message("too_close")
                return@launch
            }
            val dto = PoiInsertDto(
                name = name,
                category = category,
                latitude = latitude,
                longitude = longitude,
                is_paid = isPaid,
                description = description.ifBlank { "" },
                created_by = userId.ifBlank { null }
            )
            val created = repository.createPoi(dto)
            if (created) {
                loadPois()
                _actionResult.value = PoiActionResult.Message("created")
            } else {
                _actionResult.value = PoiActionResult.Message("create_failed")
            }
        }
    }

    fun verifyPoi(poiId: String, userId: String, userLat: Double = 0.0, userLng: Double = 0.0) {
        viewModelScope.launch {
            when (val result = repository.verifyPoi(poiId, userId)) {
                is VerifyResult.AlreadyVerified -> _actionResult.value = PoiActionResult.Message("already_verified")
                is VerifyResult.NowVerified -> {
                    _actionResult.value = PoiActionResult.Message("now_verified")
                    loadPois()
                }
                is VerifyResult.Counted -> {
                    _actionResult.value = PoiActionResult.Message("verification_counted")
                    loadPois()
                }
            }
        }
    }

    fun reportNotExist(poiId: String, userId: String, userLat: Double = 0.0, userLng: Double = 0.0) {
        viewModelScope.launch {
            when (val result = repository.reportNotExist(poiId, userId)) {
                is ReportResult.AlreadyReported -> _actionResult.value = PoiActionResult.Message("already_reported")
                is ReportResult.NowUnverified -> {
                    _actionResult.value = PoiActionResult.Message("now_unverified")
                    loadPois()
                }
                is ReportResult.Removed -> {
                    _actionResult.value = PoiActionResult.Message("removed")
                    _selectedPoi.value = null
                    loadPois()
                }
                is ReportResult.Counted -> {
                    _actionResult.value = PoiActionResult.Message("report_counted")
                    loadPois()
                }
                is ReportResult.Error -> _actionResult.value = PoiActionResult.Message("error")
            }
        }
    }

    fun checkVerificationStatus(poiId: String, userId: String) {
        viewModelScope.launch {
            val verified = repository.hasUserVerified(poiId, userId)
            val reported = repository.hasUserReported(poiId, userId)
            _actionResult.value = PoiActionResult.Status(verified, reported)
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                _searchResults.value = repository.searchPois(query)
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun findNearestPois(userLat: Double, userLng: Double, targetCategory: String? = null) {
        val all = _pois.value
        val toilet = if (targetCategory == null || targetCategory == "TOILET") {
            all.filter { it.category == "TOILET" }
                .minByOrNull { haversine(userLat, userLng, it.latitude, it.longitude) }
        } else null

        val trashCan = if (targetCategory == null || targetCategory == "TRASH_CAN") {
            all.filter { it.category == "TRASH_CAN" }
                .minByOrNull { haversine(userLat, userLng, it.latitude, it.longitude) }
        } else null

        _actionResult.value = PoiActionResult.NearestFound(toilet, trashCan, targetCategory)
    }

    fun openPoiFromDeepLink(poiId: String) {
        viewModelScope.launch {
            try {
                val poi = repository.getPoi(poiId)
                if (poi != null) {
                    selectPoi(poi)
                }
            } catch (_: Exception) { }
        }
    }

    fun refreshPoiDetails(poiId: String) {
        viewModelScope.launch {
            try {
                val poi = repository.getPoi(poiId)
                if (poi != null && _selectedPoi.value?.id == poiId) {
                    _selectedPoi.value = poi
                    _reviewCount.value = repository.getReviewCount(poiId)
                    val statusPair = reviewRepository.getLatestAnalyzedStatus(poiId)
                    _analyzedStatus.value = statusPair.first
                    _analyzedStatusTime.value = statusPair.second
                }
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
}

sealed class PoiActionResult {
    data class Message(val message: String) : PoiActionResult()
    data class Status(val hasVerified: Boolean, val hasReported: Boolean) : PoiActionResult()
    data class NearestFound(val toilet: PoiDto?, val trashCan: PoiDto?, val targetCategory: String? = null) : PoiActionResult()
}
