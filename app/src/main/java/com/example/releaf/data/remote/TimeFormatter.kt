package com.example.releaf.data.remote

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeFormatter {
    private val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")

    fun formatCommentTime(isoTime: String): String {
        if (isoTime.isBlank()) return ""
        return try {
            val instant = Instant.parse(isoTime)
            val local = instant.atZone(malaysiaZone)
            local.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
        } catch (_: Exception) {
            isoTime.take(10)
        }
    }

    fun formatHour(isoTime: String): String? {
        if (isoTime.isBlank()) return null
        return try {
            val instant = Instant.parse(isoTime)
            val local = instant.atZone(malaysiaZone)
            local.format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (_: Exception) {
            null
        }
    }
}
