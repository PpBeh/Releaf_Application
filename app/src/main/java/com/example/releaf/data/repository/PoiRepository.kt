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
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
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
        } catch (_: Exception) {
            false
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
                .select { filter { eq("poi_id", poiId) } }
                .decodeList<com.example.releaf.data.remote.dto.ReviewDto>()
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
                .select { filter { eq("user_id", userId) } }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .mapNotNull { it["poi_id"]?.toString()?.removeSurrounding("\"") }

            if (favPoiIds.isEmpty()) return emptyList()

            val allPois = client.postgrest.from("pois").select().decodeList<PoiDto>()
            allPois.filter { it.id in favPoiIds }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun verifyPoi(poiId: String, userId: String): VerifyResult {
        val existing = client.postgrest.from("poi_verifications")
            .select { filter { eq("poi_id", poiId); eq("user_id", userId); eq("action", "VERIFY") } }
            .decodeList<PoiVerificationDto>()

        if (existing.isNotEmpty()) return VerifyResult.AlreadyVerified

        client.postgrest.from("poi_verifications").insert(
            PoiVerificationDto(poi_id = poiId, user_id = userId, action = "VERIFY")
        )

        val count = client.postgrest.from("poi_verifications")
            .select { filter { eq("poi_id", poiId); eq("action", "VERIFY") } }
            .decodeList<PoiVerificationDto>().size

        if (count >= 3) {
            client.postgrest.from("pois").update(
                mapOf("is_verified" to true, "verification_count" to count)
            ) { filter { eq("id", poiId) } }
            return VerifyResult.NowVerified
        }

        client.postgrest.from("pois").update(
            mapOf("verification_count" to count)
        ) { filter { eq("id", poiId) } }

        return VerifyResult.Counted(count)
    }

    suspend fun reportNotExist(poiId: String, userId: String): ReportResult {
        val existing = client.postgrest.from("poi_verifications")
            .select { filter { eq("poi_id", poiId); eq("user_id", userId); eq("action", "REPORT") } }
            .decodeList<PoiVerificationDto>()

        if (existing.isNotEmpty()) return ReportResult.AlreadyReported

        client.postgrest.from("poi_verifications").insert(
            PoiVerificationDto(poi_id = poiId, user_id = userId, action = "REPORT")
        )

        val poi = getPoi(poiId) ?: return ReportResult.Error
        val newReportCount = poi.report_count + 1

        if (poi.is_verified && newReportCount >= 5) {
            client.postgrest.from("pois").update(
                mapOf("is_verified" to false, "verification_count" to 0, "report_count" to newReportCount)
            ) { filter { eq("id", poiId) } }
            return ReportResult.NowUnverified
        }

        if (!poi.is_verified && newReportCount >= 3) {
            client.postgrest.from("pois").delete { filter { eq("id", poiId) } }
            return ReportResult.Removed
        }

        client.postgrest.from("pois").update(
            mapOf("report_count" to newReportCount)
        ) { filter { eq("id", poiId) } }

        return ReportResult.Counted(newReportCount)
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
}

sealed class VerifyResult {
    data object AlreadyVerified : VerifyResult()
    data object NowVerified : VerifyResult()
    data class Counted(val count: Int) : VerifyResult()
}

sealed class ReportResult {
    data object AlreadyReported : ReportResult()
    data object NowUnverified : ReportResult()
    data object Removed : ReportResult()
    data class Counted(val count: Int) : ReportResult()
    data object Error : ReportResult()
}
