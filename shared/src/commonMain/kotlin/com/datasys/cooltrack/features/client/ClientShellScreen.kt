package com.datasys.cooltrack.features.client

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
import com.datasys.cooltrack.ui.components.AppNavQuickAction
import com.datasys.cooltrack.ui.components.AppNavTab
import com.datasys.cooltrack.ui.components.AppShellScaffold
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Shell de cliente con navegación real.
 */
class ClientShellScreen : Screen {
    @Composable
    override fun Content() {
        val authRepository: AuthRepository = koinInject()
        val scope = rememberCoroutineScope()

        Navigator(ClientOrdersScreen()) { navigator ->
            val current = navigator.lastItem
            AppShellScaffold(
                tabs = ClientTab.entries.map { tab ->
                    val selected = tabIndexFor(current) == tab.index
                    AppNavTab(
                        label = tab.label,
                        icon = tab.icon,
                        filledIcon = tab.filledIcon,
                        selected = selected,
                        onClick = {
                            if (tab == ClientTab.LOGOUT) {
                                scope.launch { authRepository.logout() }
                            } else if (!selected) {
                                navigator.replaceAll(tab.screen())
                            }
                        },
                    )
                },
                quickActions = listOf(
                    AppNavQuickAction("Agregar equipo", AppIcons.Equipment) {
                        navigator.push(ClientEquipmentNewScreen())
                    },
                    AppNavQuickAction("Solicitar servicio", AppIcons.Send) {
                        navigator.push(ClientRequestServiceScreen())
                    },
                ),
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    CurrentScreen()
                }
            }
        }
    }
}

private enum class ClientTab(
    val index: Int,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val screen: () -> Screen,
) {
    ORDERS(0, "Servicios", AppIcons.Orders, AppIcons.OrdersFilled, { ClientOrdersScreen() }),
    EQUIPMENT(1, "Equipos", AppIcons.Equipment, AppIcons.EquipmentFilled, { ClientEquipmentScreen() }),
    NOTIFICATIONS(2, "Alertas", AppIcons.Notifications, AppIcons.NotificationsFilled, { NotificationsScreen() }),
    LOGOUT(3, "Salir", AppIcons.Logout, AppIcons.Logout, { ClientOrdersScreen() }),
}

private fun tabIndexFor(screen: Screen): Int = when (screen) {
    is ClientOrdersScreen, is ClientRequestServiceScreen, is ClientOrderDetailScreen -> 0
    is ClientEquipmentScreen, is ClientEquipmentNewScreen -> 1
    is NotificationsScreen -> 2
    else -> 0
}
