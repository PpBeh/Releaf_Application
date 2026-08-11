package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.PoiDto
import com.example.releaf.data.remote.dto.PoiInsertDto
import com.example.releaf.data.remote.dto.PoiPhotoDto
import com.example.releaf.data.repository.PoiRepository
import com.example.releaf.data.repository.ReportResult
import com.example.releaf.data.repository.VerifyResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private val repository = PoiRepository()

    private val _pois = MutableStateFlow<List<PoiDto>>(emptyList())
    val pois: StateFlow<List<PoiDto>> = _pois.asStateFlow()

    private val _selectedPoi = MutableStateFlow<PoiDto?>(null)
    val selectedPoi: StateFlow<PoiDto?> = _selectedPoi.asStateFlow()

    private val _filteredPois = MutableStateFlow<List<PoiDto>>(emptyList())
    val filteredPois: StateFlow<List<PoiDto>> = _filteredPois.asStateFlow()

    private val _photos = MutableStateFlow<List<PoiPhotoDto>>(emptyList())
    val photos: StateFlow<List<PoiPhotoDto>> = _photos.asStateFlow()

    private val _categoryFilter = MutableStateFlow("ALL")
    private val _cleanlinessFilter = MutableStateFlow("ALL")
    private val _paidFilter = MutableStateFlow("ALL")

    private val _actionResult = MutableStateFlow<PoiActionResult?>(null)
    val actionResult: StateFlow<PoiActionResult?> = _actionResult.asStateFlow()

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
            _photos.value = repository.getPoiPhotos(poi.id)
        }
    }

    fun clearSelection() {
        _selectedPoi.value = null
        _photos.value = emptyList()
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
        applyFilters()
    }

    fun setCleanlinessFilter(cleanliness: String) {
        _cleanlinessFilter.value = cleanliness
        applyFilters()
    }

    fun setPaidFilter(paid: String) {
        _paidFilter.value = paid
        applyFilters()
    }

    private fun applyFilters() {
        var result = _pois.value
        if (_categoryFilter.value != "ALL") {
            result = result.filter { it.category == _categoryFilter.value }
        }
        if (_cleanlinessFilter.value != "ALL") {
            result = result.filter { it.cleanliness == _cleanlinessFilter.value }
        }
        if (_paidFilter.value != "ALL") {
            val isPaid = _paidFilter.value == "PAID"
            result = result.filter { it.is_paid == isPaid }
        }
        _filteredPois.value = result
    }

    fun createPoi(name: String, category: String, latitude: Double, longitude: Double, isPaid: Boolean, userId: String, description: String = "") {
        viewModelScope.launch {
            val dto = PoiInsertDto(
                name = name,
                category = category,
                latitude = latitude,
                longitude = longitude,
                is_paid = isPaid,
                description = description.ifBlank { "" },
                created_by = userId.ifBlank { null }
            )
            try {
                val created = repository.createPoi(dto)
                if (created != null) {
                    loadPois()
                    _actionResult.value = PoiActionResult.Message("created")
                } else {
                    _actionResult.value = PoiActionResult.Message("create_failed")
                }
            } catch (e: Exception) {
                _actionResult.value = PoiActionResult.Message("error_${e.message}")
            }
        }
    }

    fun verifyPoi(poiId: String, userId: String) {
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

    fun reportNotExist(poiId: String, userId: String) {
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
}

sealed class PoiActionResult {
    data class Message(val message: String) : PoiActionResult()
    data class Status(val hasVerified: Boolean, val hasReported: Boolean) : PoiActionResult()
}
