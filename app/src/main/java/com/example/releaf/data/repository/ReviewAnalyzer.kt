package com.example.releaf.data.repository

object ReviewAnalyzer {
    fun analyze(text: String, starRating: Int): String? {
        val lower = text.lowercase()

        val closedKeywords = listOf(
            "closed", "tutup", "shut down", "locked",
            "cannot use", "can't use", "cant use", "not open", "not opened",
            "out of service", "out of order", "unavailable", "no access"
        )
        val cleaningKeywords = listOf(
            "cleaning", "washing", "cuci", "maintenance",
            "under repair", "being cleaned", "mopping"
        )
        val brokenKeywords = listOf(
            "broken", "not working", "doesn't work", "doesnt work", "spoiled",
            "rosak", "damaged", "leaking", "flooded", "clogged", "blocked"
        )
        val noWaterKeywords = listOf(
            "no water", "no air", "water not working", "no tap", "water problem",
            "water out"
        )
        val crowdedKeywords = listOf(
            "crowded", "queue", "long queue", "waiting", "busy"
        )
        // Positive phrases that should not be read as negative.
        val isCleanKeywords = listOf("is clean", "very clean", "so clean", "super clean", "clean toilet", "clean and", "clean room", "clean place", "spotless", "tidy")

        val hasClosed = closedKeywords.any { containsWord(lower, it) }
        val hasCleaning = cleaningKeywords.any { containsWord(lower, it) }
        val hasBroken = brokenKeywords.any { containsWord(lower, it) }
        val hasNoWater = noWaterKeywords.any { containsWord(lower, it) }
        val hasCrowded = crowdedKeywords.any { containsWord(lower, it) }
        val saysClean = isCleanKeywords.any { containsWord(lower, it) }

        return when {
            hasClosed && hasCleaning -> "might be closed for cleaning"
            hasClosed -> "might be closed"
            hasCleaning && !saysClean -> "might be cleaning"
            hasBroken -> "might be out of service"
            hasNoWater -> "might have no water"
            hasCrowded && starRating <= 3 -> "might be crowded right now"
            else -> null
        }
    }

    private fun containsWord(text: String, keyword: String): Boolean {
        if (keyword.isBlank()) return false
        val escaped = java.util.regex.Pattern.quote(keyword)
        return Regex("(?<![A-Za-z0-9])$escaped(?![A-Za-z0-9])").containsMatchIn(text)
    }
}
