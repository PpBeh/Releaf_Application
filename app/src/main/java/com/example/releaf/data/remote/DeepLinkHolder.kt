package com.example.releaf.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DeepLinkHolder {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var type: String? = null
    var pendingName: String? = null
    var pendingEmail: String? = null

    private val _pendingPoiId = MutableStateFlow<String?>(null)
    val pendingPoiIdFlow: StateFlow<String?> = _pendingPoiId.asStateFlow()

    var pendingPoiId: String?
        get() = _pendingPoiId.value
        set(value) { _pendingPoiId.value = value }

    @Synchronized
    fun consumeTokens(): Triple<String?, String?, String?> {
        val tokens = Triple(accessToken, refreshToken, type)
        accessToken = null
        refreshToken = null
        type = null
        return tokens
    }

    @Synchronized
    fun setTokens(access: String, refresh: String, tokenType: String?) {
        accessToken = access
        refreshToken = refresh
        type = tokenType
    }

    fun clear() {
        accessToken = null
        refreshToken = null
        type = null
        pendingName = null
        pendingEmail = null
    }

    fun clearPoiId() {
        _pendingPoiId.value = null
    }
}
