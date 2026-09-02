package com.datasys.cooltrack.features.tech

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.features.notifications.NotificationsScreen
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppNavTab
import com.datasys.cooltrack.ui.components.AppShellScaffold
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Shell de técnico con navegación real.
 */
class TechnicianShellScreen : Screen {
    @Composable
    override fun Content() {
        val authRepository: AuthRepository = koinInject()
        val scope = rememberCoroutineScope()

        Navigator(TechnicianJobsScreen()) { navigator ->
            val current = navigator.lastItem
            AppShellScaffold(
                tabs = TechTab.entries.map { tab ->
                    val selected = tabIndexFor(current) == tab.index
                    AppNavTab(
                        label = tab.label,
                        icon = tab.icon,
                        filledIcon = tab.filledIcon,
                        selected = selected,
                        onClick = {
                            if (tab == TechTab.LOGOUT) {
                                scope.launch { authRepository.logout() }
                            } else if (!selected) {
                                navigator.replaceAll(tab.screen())
                            }
                        },
                    )
                },
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    CurrentScreen()
                }
            }
        }
    }
}

private enum class TechTab(
    val index: Int,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val screen: () -> Screen,
) {
    JOBS(0, "Trabajos", AppIcons.Orders, AppIcons.OrdersFilled, { TechnicianJobsScreen() }),
    NOTIFICATIONS(1, "Notificaciones", AppIcons.Notifications, AppIcons.NotificationsFilled, { NotificationsScreen() }),
    LOGOUT(2, "Salir", AppIcons.Logout, AppIcons.Logout, { TechnicianJobsScreen() }),
}

private fun tabIndexFor(screen: Screen): Int = when (screen) {
    is TechnicianJobsScreen, is TechnicianJobDetailScreen -> 0
    is NotificationsScreen -> 1
    else -> 0
}
