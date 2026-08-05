package com.example.releaf.model

/**
 * Shared shape for map points of interest — toilets and trash cans
 * both use this, since the detail sheet / direction / comment flow
 * is identical for either category.
 */
enum class PoiCategory {
    TOILET,
    TRASH_CAN
}

enum class CleanlinessStatus {
    CLEAN,
    AVERAGE,
    DIRTY
}

data class Poi(
    val id: String,
    val name: String,
    val category: PoiCategory,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val cleanliness: CleanlinessStatus,
    val photoUrls: List<String> = emptyList()
) {
    companion object {
        // TODO: replace with a real data source (API / database) later
        fun sampleList(): List<Poi> = emptyList()
    }
}