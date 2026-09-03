package com.example.releaf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.releaf.data.remote.dto.NotificationDto
import com.example.releaf.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {
    private val repository = NotificationRepository()

    private val _notifications = MutableStateFlow<List<NotificationDto>>(emptyList())
    val notifications: StateFlow<List<NotificationDto>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun loadNotifications(userId: String) {
        viewModelScope.launch {
            try {
                val list = repository.getNotifications(userId)
                _notifications.value = list
                _unreadCount.value = list.count { !it.is_read && it.user_id != null }
            } catch (_: Exception) {
            }
        }
    }

    fun markAsRead(notification: NotificationDto, userId: String) {
        viewModelScope.launch {
            try {
                // Global announcements are shared by every user — never mark them
                // as read on behalf of one user.
                if (notification.user_id == null) return@launch
                repository.markAsRead(notification.id)
                loadNotifications(userId)
            } catch (_: Exception) {
            }
        }
    }

    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            try {
                repository.markAllAsRead(userId)
                loadNotifications(userId)
            } catch (_: Exception) {
            }
        }
    }

    fun checkRewardNotifications(userId: String, userPoints: Int) {
        viewModelScope.launch {
            try {
                val existing = repository.getNotifications(userId)
                val seeds = com.example.releaf.model.SeedData.seedList
                seeds.forEach { seed ->
                    if (userPoints >= seed.targetPoints) {
                        val title = "🎁 Reward Available: ${seed.name}"
                        if (existing.none { it.title == title }) {
                            repository.sendNotification(
                                userId = userId,
                                title = title,
                                body = "Congratulations! You reached ${seed.targetPoints} EXP and unlocked the ${seed.name} seed. Visit Rewards to claim your seedling!",
                                type = "REWARD"
                            )
                        }
                    }
                }

                // Check Badge milestones
                val badges = listOf(
                    500 to "Bronze Gardener Badge",
                    2000 to "Silver Gardener Badge",
                    10000 to "Gold Gardener Badge"
                )
                badges.forEach { (pts, label) ->
                    if (userPoints >= pts) {
                        val title = "🏆 Badge Milestone: $label"
                        if (existing.none { it.title == title }) {
                            repository.sendNotification(
                                userId = userId,
                                title = title,
                                body = "Amazing! You accumulated $userPoints EXP and unlocked the $label!",
                                type = "REWARD"
                            )
                        }
                    }
                }

                loadNotifications(userId)
            } catch (_: Exception) {
            }
        }
    }
}
