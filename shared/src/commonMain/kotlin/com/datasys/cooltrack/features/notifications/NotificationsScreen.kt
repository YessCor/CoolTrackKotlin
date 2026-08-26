package com.datasys.cooltrack.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.NotificationType
import com.datasys.cooltrack.models.AppNotification
import com.datasys.cooltrack.notifications.NotificationRepository
import com.datasys.cooltrack.ui.components.AppTopBar
import com.datasys.cooltrack.ui.components.AppEmptyState
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pantalla de notificaciones con lista, mark-as-read y actualizaciones en tiempo real.
 * Equivalente a features/notifications/views/notifications_screen.dart.
 */
class NotificationsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val notificationRepository: NotificationRepository = koinInject()
        val scope = rememberCoroutineScope()

        val notifications by notificationRepository.notifications.collectAsStateSimple()
        val unreadCount by notificationRepository.unreadCount.collectAsStateSimple()

        LaunchedEffect(Unit) {
            notificationRepository.start()
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    expandedHeight = 44.dp,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notificaciones")
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.Error),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else "$unreadCount",
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = AppIcons.ArrowBack, contentDescription = "Volver")
                        }
                    },
                )
            },
        ) { padding ->
            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    AppEmptyState(
                        icon = AppIcons.Notifications,
                        title = "Sin notificaciones",
                        message = "No tienes notificaciones nuevas",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(notifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onTap = {
                                if (!notification.isRead) {
                                    scope.launch {
                                        notificationRepository.markAsRead(notification.id)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: AppNotification,
    onTap: () -> Unit,
) {
    val icon = when (notification.type) {
        NotificationType.ORDER -> AppIcons.Orders
        NotificationType.QUOTE -> AppIcons.Quotes
        NotificationType.INFO -> AppIcons.Info
        NotificationType.ALERT -> AppIcons.Warning
    }

    val iconTint = when (notification.type) {
        NotificationType.ORDER -> AppColors.Info
        NotificationType.QUOTE -> AppColors.Secondary
        NotificationType.INFO -> AppColors.TextMuted
        NotificationType.ALERT -> AppColors.Warning
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .background(
                if (notification.isRead) AppColors.Surface else AppColors.Secondary.copy(alpha = 0.05f)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = notification.title,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppColors.Secondary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.message,
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                maxLines = 2,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatNotificationTime(notification.createdAt.toString()),
                fontSize = 12.sp,
                color = AppColors.TextMuted,
            )
        }
    }
}

private fun formatNotificationTime(isoString: String): String {
    return try {
        val datePart = isoString.split("T").firstOrNull() ?: return isoString
        val timePart = isoString.split("T").getOrNull(1)?.split(".")?.firstOrNull() ?: ""
        if (timePart.isNotEmpty()) "$datePart $timePart" else datePart
    } catch (_: Exception) {
        isoString
    }
}
