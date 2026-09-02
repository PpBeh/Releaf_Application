package com.example.releaf.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DeepLinkHolder {
    var accessToken: String? = null
    var refreshToken: String? = null
    var type: String? = null
    var pendingName: String? = null

    private val _pendingPoiId = MutableStateFlow<String?>(null)
    val pendingPoiIdFlow: StateFlow<String?> = _pendingPoiId.asStateFlow()

    var pendingPoiId: String?
        get() = _pendingPoiId.value
        set(value) { _pendingPoiId.value = value }

    fun clear() {
        accessToken = null
        refreshToken = null
        type = null
    }

    fun clearPoiId() {
        _pendingPoiId.value = null
    }
}
