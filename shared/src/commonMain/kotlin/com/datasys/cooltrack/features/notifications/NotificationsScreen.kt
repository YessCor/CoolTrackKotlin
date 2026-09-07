package com.datasys.cooltrack.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.NotificationType
import com.datasys.cooltrack.models.AppNotification
import com.datasys.cooltrack.notifications.NotificationRepository
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppHeroChip
import com.datasys.cooltrack.ui.components.AppHeroListScaffold
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppEmptyState
import com.datasys.cooltrack.ui.components.appEnter
import com.datasys.cooltrack.ui.components.pressable
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Notificaciones con hero compacto (contador de no leídas) e "inbox" de
 * tarjetas: las no leídas llevan un borde índigo y un punto.
 */
class NotificationsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val notificationRepository: NotificationRepository = koinInject()
        val scope = rememberCoroutineScope()

        val notifications by notificationRepository.notifications.collectAsStateSimple()
        val unreadCount by notificationRepository.unreadCount.collectAsStateSimple()

        LaunchedEffect(Unit) { notificationRepository.start() }

        AppHeroListScaffold(
            title = "Notificaciones",
            subtitle = if (unreadCount > 0) "$unreadCount sin leer" else "Al día",
            onBack = if (navigator.canPop) ({ navigator.pop() }) else null,
            heroTrailing = if (unreadCount > 0) ({ AppHeroChip(if (unreadCount > 99) "99+" else "$unreadCount") }) else null,
        ) {
            if (notifications.isEmpty()) {
                item {
                    AppEmptyState(
                        icon = AppIcons.Notifications,
                        title = "Sin notificaciones",
                        message = "No tienes notificaciones nuevas.",
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            } else {
                items(notifications.size) { index ->
                    val n = notifications[index]
                    NotificationCard(n, Modifier.appEnter(index)) {
                        if (!n.isRead) scope.launch { notificationRepository.markAsRead(n.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(n: AppNotification, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val icon = when (n.type) {
        NotificationType.ORDER -> AppIcons.Orders
        NotificationType.QUOTE -> AppIcons.Quotes
        NotificationType.INFO -> AppIcons.Info
        NotificationType.ALERT -> AppIcons.Warning
    }
    val tint = when (n.type) {
        NotificationType.ORDER -> AppColors.Info
        NotificationType.QUOTE -> AppColors.Secondary
        NotificationType.INFO -> AppColors.TextMuted
        NotificationType.ALERT -> AppColors.Warning
    }
    AppCard(
        modifier = modifier,
        color = if (n.isRead) AppColors.Surface else AppColors.Secondary.copy(alpha = 0.06f),
        onTap = onTap,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        n.title,
                        fontWeight = if (n.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (!n.isRead) Box(Modifier.size(8.dp).clip(CircleShape).background(AppColors.Secondary))
                }
                Spacer(Modifier.height(4.dp))
                Text(n.message, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(formatNotificationTime(n.createdAt.toString()), style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted)
            }
        }
    }
}

private fun formatNotificationTime(isoString: String): String {
    return try {
        val datePart = isoString.substringBefore("T")
        val timePart = isoString.substringAfter("T", "").substringBefore(".").take(5)
        if (timePart.isNotEmpty()) "$datePart · $timePart" else datePart
    } catch (_: Exception) {
        isoString
    }
}
