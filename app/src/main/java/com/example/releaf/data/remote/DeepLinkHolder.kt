package com.example.releaf.data.remote

object DeepLinkHolder {
    var accessToken: String? = null
    var refreshToken: String? = null
    var type: String? = null

    fun clear() {
        accessToken = null
        refreshToken = null
        type = null
    }
}
