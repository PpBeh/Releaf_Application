package com.example.releaf.data.repository

object ReviewAnalyzer {
    fun analyze(text: String, starRating: Int): String? {
        val lower = text.lowercase()

        val closedKeywords = listOf(
            "closed", "close", "tutup", "shut down", "shut", "locked",
            "cannot use", "can't use", "cant use", "not open", "not opened",
            "out of service", "out of order", "unavailable", "no access"
        )
        val cleaningKeywords = listOf(
            "cleaning", "clean", "washing", "wash", "cuci", "maintenance",
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
            "full", "crowded", "queue", "long queue", "waiting", "wait", "busy"
        )

        val hasClosed = closedKeywords.any { lower.contains(it) }
        val hasCleaning = cleaningKeywords.any { lower.contains(it) }
        val hasBroken = brokenKeywords.any { lower.contains(it) }
        val hasNoWater = noWaterKeywords.any { lower.contains(it) }
        val hasCrowded = crowdedKeywords.any { lower.contains(it) }

        return when {
            hasClosed && hasCleaning -> "might be closed for cleaning"
            hasClosed -> "might be closed"
            hasCleaning -> "might be cleaning"
            hasBroken -> "might be out of service"
            hasNoWater -> "might have no water"
            hasCrowded && starRating <= 3 -> "might be crowded right now"
            else -> null
        }
    }
}
