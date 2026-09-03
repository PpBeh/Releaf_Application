package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.PoiDto
import com.example.releaf.data.repository.PoiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavouritesViewModel : ViewModel() {
    private val repository = PoiRepository()

    private val _favorites = MutableStateFlow<List<PoiDto>>(emptyList())
    val favorites: StateFlow<List<PoiDto>> = _favorites.asStateFlow()

    fun loadFavorites(userId: String) {
        viewModelScope.launch {
            try {
                _favorites.value = repository.getFavoritePois(userId)
            } catch (_: Exception) {
            }
        }
    }

    fun removeFavorite(poiId: String, userId: String) {
        viewModelScope.launch {
            try {
                // Optimistically remove so a double-tap cannot re-add the item.
                _favorites.value = _favorites.value.filterNot { it.id == poiId }
                repository.removeFavorite(poiId, userId)
                _favorites.value = repository.getFavoritePois(userId)
            } catch (_: Exception) {
            }
        }
    }
}
