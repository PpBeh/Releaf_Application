package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.PoiDto
import com.example.releaf.data.remote.dto.PoiInsertDto
import com.example.releaf.data.remote.dto.PoiPhotoDto
import com.example.releaf.data.remote.dto.PoiVerificationDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage

class PoiRepository {
    private val client = SupabaseModule.client

    suspend fun getAllPois(): List<PoiDto> {
        return client.postgrest.from("pois").select().decodeList()
    }

    suspend fun getPoi(poiId: String): PoiDto? {
        return client.postgrest.from("pois")
            .select { filter { eq("id", poiId) } }
            .decodeSingleOrNull()
    }

    suspend fun searchPois(query: String): List<PoiDto> {
        if (query.isBlank()) return emptyList()
        return try {
            // Try server-side ilike on name OR description
            try {
                client.postgrest.from("pois")
                    .select {
                        filter {
                            or {
                                ilike("name", "%$query%")
                                ilike("description", "%$query%")
                            }
                        }
                    }
                    .decodeList<PoiDto>()
            } catch (_: Exception) {
                // Fallback: filter locally after fetching
                val all = client.postgrest.from("pois").select().decodeList<PoiDto>()
                all.filter { it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createPoi(poi: PoiInsertDto): Boolean {
        return try {
            client.postgrest.from("pois").insert(poi)
            true
        } catch (e: Exception) {
            if (e.message?.contains("duplicate") == true) {
                true
            } else {
                false
            }
        }
    }

    suspend fun getPoiPhotos(poiId: String): List<PoiPhotoDto> {
        return client.postgrest.from("poi_photos")
            .select { filter { eq("poi_id", poiId) } }
            .decodeList()
    }

    suspend fun uploadPoiPhoto(poiId: String, userId: String, uri: android.net.Uri, context: android.content.Context): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bytes: ByteArray = run {
                    var result: ByteArray? = null
                    try {
                        result = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    } catch (_: Exception) { }

                    if (result == null && uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                        try {
                            result = java.io.File(uri.path!!).readBytes()
                        } catch (_: Exception) { }
                    }
                    result
                } ?: return@withContext false

                val fileName = "${poiId}_${System.currentTimeMillis()}.jpg"
                client.storage.from("poi-photos").upload(
                    path = fileName,
                    data = bytes
                ) {
                    upsert = true
                }
                val url = client.storage.from("poi-photos").publicUrl(fileName)
                client.postgrest.from("poi_photos").insert(
                    PoiPhotoDto(poi_id = poiId, photo_url = url, uploaded_by = userId)
                )
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun addPoiPhoto(poiId: String, photoUrl: String, userId: String) {
        client.postgrest.from("poi_photos").insert(
            PoiPhotoDto(poi_id = poiId, photo_url = photoUrl, uploaded_by = userId)
        )
    }

    suspend fun getReviewCount(poiId: String): Int {
        return try {
            val reviews = client.postgrest.from("reviews")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("id")) { filter { eq("poi_id", poiId) } }
                .decodeList<kotlinx.serialization.json.JsonObject>()
            reviews.size
        } catch (_: Exception) {
            0
        }
    }

    suspend fun isFavorite(poiId: String, userId: String): Boolean {
        return try {
            val favs = client.postgrest.from("favorites")
                .select { filter { eq("poi_id", poiId); eq("user_id", userId) } }
                .decodeList<kotlinx.serialization.json.JsonObject>()
            favs.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun toggleFavorite(poiId: String, userId: String): Boolean {
        val currentlyFav = isFavorite(poiId, userId)
        return try {
            if (currentlyFav) {
                client.postgrest.from("favorites").delete {
                    filter { eq("poi_id", poiId); eq("user_id", userId) }
                }
                false
            } else {
                client.postgrest.from("favorites").insert(
                    mapOf("poi_id" to poiId, "user_id" to userId)
                )
                true
            }
        } catch (_: Exception) {
            currentlyFav
        }
    }

    suspend fun getFavoritePois(userId: String): List<PoiDto> {
        return try {
            val favPoiIds = client.postgrest.from("favorites")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("poi_id")) { filter { eq("user_id", userId) } }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .mapNotNull { it["poi_id"]?.toString()?.removeSurrounding("\"") }

            if (favPoiIds.isEmpty()) return emptyList()

            val allPois = client.postgrest.from("pois").select().decodeList<PoiDto>()
            allPois.filter { it.id in favPoiIds }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun removeFavorite(poiId: String, userId: String) {
        try {
            client.postgrest.from("favorites").delete {
                filter { eq("poi_id", poiId); eq("user_id", userId) }
            }
        } catch (_: Exception) { }
    }

    suspend fun verifyPoi(poiId: String, userId: String): VerifyResult {
        return try {
            val existing = client.postgrest.from("poi_verifications")
                .select { filter { eq("poi_id", poiId); eq("user_id", userId); eq("action", "VERIFY") } }
                .decodeList<PoiVerificationDto>()

            if (existing.isNotEmpty()) return VerifyResult.AlreadyVerified

            client.postgrest.from("poi_verifications").insert(
                PoiVerificationDto(poi_id = poiId, user_id = userId, action = "VERIFY")
            )

            // The database trigger updates the counters asynchronously; poll briefly
            // instead of sleeping a fixed amount and guessing.
            var updated = getPoi(poiId)
            repeat(4) {
                updated = getPoi(poiId)
                val u = updated
                if (u != null && u.verification_count > 0) return@repeat
                kotlinx.coroutines.delay(300)
            }
            val poi = updated
            if (poi == null) return VerifyResult.Error
            if (poi.is_verified) {
                VerifyResult.NowVerified
            } else {
                VerifyResult.Counted(poi.verification_count)
            }
        } catch (_: Exception) {
            VerifyResult.Error
        }
    }

    suspend fun reportNotExist(poiId: String, userId: String): ReportResult {
        return try {
            val existing = client.postgrest.from("poi_verifications")
                .select { filter { eq("poi_id", poiId); eq("user_id", userId); eq("action", "REPORT") } }
                .decodeList<PoiVerificationDto>()

            if (existing.isNotEmpty()) return ReportResult.AlreadyReported

            client.postgrest.from("poi_verifications").insert(
                PoiVerificationDto(poi_id = poiId, user_id = userId, action = "REPORT")
            )

            // The database trigger updates the counters asynchronously; poll briefly.
            var updated = getPoi(poiId)
            repeat(4) {
                updated = getPoi(poiId)
                val u = updated
                if (u != null && u.report_count > 0) return@repeat
                kotlinx.coroutines.delay(300)
            }
            val poi = updated ?: return ReportResult.Removed

            // Five users say it doesn't exist and nobody has ever verified it -> remove.
            if (!poi.is_verified && poi.verification_count == 0 && poi.report_count >= 5) {
                try {
                    client.postgrest.from("pois").delete { filter { eq("id", poiId) } }
                } catch (_: Exception) { }
                ReportResult.Removed
            } else if (!poi.is_verified && poi.report_count >= 3) {
                ReportResult.NowUnverified
            } else {
                ReportResult.Counted(poi.report_count)
            }
        } catch (_: Exception) {
            ReportResult.Error
        }
    }

    suspend fun hasUserVerified(poiId: String, userId: String): Boolean {
        val result = client.postgrest.from("poi_verifications")
            .select { filter { eq("poi_id", poiId); eq("user_id", userId); eq("action", "VERIFY") } }
            .decodeList<PoiVerificationDto>()
        return result.isNotEmpty()
    }

    suspend fun hasUserReported(poiId: String, userId: String): Boolean {
        val result = client.postgrest.from("poi_verifications")
            .select { filter { eq("poi_id", poiId); eq("user_id", userId); eq("action", "REPORT") } }
            .decodeList<PoiVerificationDto>()
        return result.isNotEmpty()
    }

    suspend fun countUserVerifications(userId: String): Int {
        return try {
            client.postgrest.from("poi_verifications")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("id")) {
                    filter { eq("user_id", userId); eq("action", "VERIFY") }
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .size
        } catch (_: Exception) {
            0
        }
    }

    suspend fun countUserPhotos(userId: String): Int {
        return try {
            client.postgrest.from("poi_photos")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw("id")) {
                    filter { eq("uploaded_by", userId) }
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .size
        } catch (_: Exception) {
            0
        }
    }
}

sealed class VerifyResult {
    data object AlreadyVerified : VerifyResult()
    data object NowVerified : VerifyResult()
    data class Counted(val count: Int) : VerifyResult()
    data object Error : VerifyResult()
}

sealed class ReportResult {
    data object AlreadyReported : ReportResult()
    data object NowUnverified : ReportResult()
    data object Removed : ReportResult()
    data class Counted(val count: Int) : ReportResult()
    data object Error : ReportResult()
}
