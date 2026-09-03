package com.example.releaf.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.releaf.R

object NotificationHelper {
    private const val CHANNEL_ID = "releaf_system_alerts"

    /**
     * Fixed ID for the aggregated summary. Re-posting with the same ID updates
     * the existing shade entry in place instead of stacking new ones.
     */
    private const val SUMMARY_ID = 1001

    const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Releaf Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "System notifications for Releaf"
            }
            manager(context).createNotificationChannel(channel)
        }
    }

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showNotification(context: Context, title: String, message: String) {
        if (!canPost(context)) return
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            manager(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: Exception) {
        }
    }

    /**
     * Posts (or updates in place) the single aggregated summary, e.g.
     * "You have 3 notifications!". Pass [alert] = true to buzz once for
     * genuinely new arrivals; false refreshes the text silently.
     */
    fun showOrUpdateSummary(context: Context, title: String, message: String, alert: Boolean) {
        if (!canPost(context)) return
        ensureChannel(context)

        // Tapping opens the app straight into the in-app notifications sheet.
        val tapIntent = android.content.Intent(
            context,
            com.example.releaf.MainActivity::class.java
        ).apply {
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
        }
        val tapPendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPendingIntent)
            .setOnlyAlertOnce(!alert)
            .setAutoCancel(true)
            .build()

        try {
            manager(context).notify(SUMMARY_ID, notification)
        } catch (_: Exception) {
        }
    }

    fun cancelSummary(context: Context) {
        try {
            manager(context).cancel(SUMMARY_ID)
        } catch (_: Exception) {
        }
    }
}
