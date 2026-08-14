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
            } catch (_: Exception) { }
        }
    }

    fun markAsRead(notification: NotificationDto, userId: String) {
        viewModelScope.launch {
            try {
                repository.markAsRead(notification.id)
                loadNotifications(userId)
            } catch (_: Exception) { }
        }
    }

    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            try {
                repository.markAllAsRead(userId)
                loadNotifications(userId)
            } catch (_: Exception) { }
        }
    }
}
