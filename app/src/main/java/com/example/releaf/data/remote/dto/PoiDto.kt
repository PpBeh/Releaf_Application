package com.example.releaf.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PoiDto(
    val id: String,
    val name: String,
    val category: String = "TOILET",
    val latitude: Double,
    val longitude: Double,
    val rating: Double = 0.0,
    val cleanliness: String = "CLEAN",
    val is_paid: Boolean = false,
    val is_verified: Boolean = false,
    val verification_count: Int = 0,
    val report_count: Int = 0,
    val created_by: String? = null,
    val description: String = "",
    val recent_status: String? = null,
    val recent_status_time: String? = null,
    val photo_urls: List<String> = emptyList(),
    val created_at: String = ""
) {
    // Needed for serialisation defaults when columns missing
}

@Serializable
data class PoiInsertDto(
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val is_paid: Boolean = false,
    val description: String = "",
    val created_by: String? = null
)

@Serializable
data class PoiVerificationDto(
    val id: String = "",
    val poi_id: String,
    val user_id: String,
    val action: String,
    val created_at: String = ""
)

@Serializable
data class PoiPhotoDto(
    val id: String = "",
    val poi_id: String,
    val photo_url: String,
    val uploaded_by: String = "",
    val created_at: String = ""
)
