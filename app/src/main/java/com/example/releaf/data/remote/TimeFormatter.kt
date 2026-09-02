package com.example.releaf.data.remote

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object TimeFormatter {
    private val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")
    private val commentFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    private val hourFormatter = DateTimeFormatter.ofPattern("h:mm a")

    private fun parseToInstant(isoTime: String): Instant? {
        if (isoTime.isBlank()) return null
        // Try Instant (requires Z)
        try { return Instant.parse(isoTime) } catch (_: Exception) {}
        // Try OffsetDateTime (handles +08:00)
        try { return OffsetDateTime.parse(isoTime).toInstant() } catch (_: Exception) {}
        // Try without TZ, assume UTC
        try {
            val ldt = java.time.LocalDateTime.parse(isoTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            return ldt.atZone(ZoneId.of("UTC")).toInstant()
        } catch (_: Exception) {}
        // Try with millis and offset without colon
        try {
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS][.SSS][.SS][.S][XXX][X]")
            return OffsetDateTime.parse(isoTime, fmt).toInstant()
        } catch (_: Exception) {}
        return null
    }

    fun formatCommentTime(isoTime: String): String {
        if (isoTime.isBlank()) return ""
        val instant = parseToInstant(isoTime)
        return if (instant != null) {
            try {
                val local = instant.atZone(malaysiaZone)
                local.format(commentFormatter)
            } catch (_: Exception) { isoTime.take(10) }
        } else {
            // fallback: try to extract date part
            try { isoTime.substring(0, 10) } catch (_: Exception) { isoTime.take(10) }
        }
    }

    fun formatHour(isoTime: String): String? {
        if (isoTime.isBlank()) return null
        val instant = parseToInstant(isoTime) ?: return null
        return try {
            val local = instant.atZone(malaysiaZone)
            local.format(hourFormatter)
        } catch (_: Exception) {
            null
        }
    }
}
