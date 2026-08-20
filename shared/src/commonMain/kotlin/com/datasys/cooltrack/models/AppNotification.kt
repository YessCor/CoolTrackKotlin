package com.datasys.cooltrack.models

import com.datasys.cooltrack.core.NotificationType
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Equivalente a lib/models/notification.dart.
 * Se renombra a AppNotification (igual que en Dart) para no chocar con
 * clases de notificación del sistema en Android/iOS.
 */
@Serializable
data class AppNotification(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: Instant,
)
