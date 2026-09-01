package com.example.releaf.data.repository

import com.example.releaf.data.remote.SupabaseModule
import com.example.releaf.data.remote.dto.NotificationDto
import io.github.jan.supabase.postgrest.postgrest

class NotificationRepository {
    private val client = SupabaseModule.client

    suspend fun getNotifications(userId: String): List<NotificationDto> {
        return try {
            val personal = client.postgrest.from("notifications")
                .select { filter { eq("user_id", userId) } }
                .decodeList<NotificationDto>()
            val announcements = try {
                client.postgrest.from("notifications")
                    .select { filter { eq("type", "ANNOUNCEMENT") } }
                    .decodeList<NotificationDto>()
            } catch (_: Exception) {
                emptyList()
            }
            (personal + announcements).sortedByDescending { it.created_at }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun markAsRead(notificationId: String) {
        try {
            client.postgrest.from("notifications").update(
                mapOf("is_read" to true)
            ) { filter { eq("id", notificationId) } }
        } catch (_: Exception) { }
    }

    suspend fun markAllAsRead(userId: String) {
        try {
            client.postgrest.from("notifications").update(
                mapOf("is_read" to true)
            ) { filter { eq("user_id", userId) } }
        } catch (_: Exception) { }
    }

    suspend fun sendNotification(userId: String, title: String, body: String, type: String = "REWARD") {
        try {
            val data = mapOf(
                "user_id" to userId,
                "title" to title,
                "body" to body,
                "type" to type,
                "is_read" to false
            )
            client.postgrest.from("notifications").insert(data)
        } catch (_: Exception) { }
    }
}
