package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.datasys.cooltrack.features.notifications.NotificationsScreen
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppNavQuickAction
import com.datasys.cooltrack.ui.components.AppNavTab
import com.datasys.cooltrack.ui.components.AppShellScaffold

class AdminShellScreen : Screen {
    @Composable
    override fun Content() {
        Navigator(AdminDashboardScreen()) { navigator ->
            val current = navigator.lastItem
            AppShellScaffold(
                tabs = AdminTab.entries.map { tab ->
                    val selected = tabIndexFor(current) == tab.index
                    AppNavTab(
                        label = tab.label,
                        icon = tab.icon,
                        filledIcon = tab.filledIcon,
                        selected = selected,
                        onClick = { if (!selected) navigator.replaceAll(tab.screen()) },
                    )
                },
                quickActions = listOf(
                    AppNavQuickAction("Nueva cotización", AppIcons.Quotes) {
                        navigator.push(AdminQuoteNewScreen())
                    },
                    AppNavQuickAction("Nuevo técnico", AppIcons.Technicians) {
                        navigator.push(AdminCreateTechnicianScreen())
                    },
                    AppNavQuickAction("Nuevo cliente", AppIcons.PersonAdd) {
                        navigator.push(AdminClientNewScreen())
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

private enum class AdminTab(
    val index: Int,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val screen: () -> Screen,
) {
    DASHBOARD(0, "Dashboard", AppIcons.Dashboard, AppIcons.DashboardFilled, { AdminDashboardScreen() }),
    CLIENTS(1, "Clientes", AppIcons.Clients, AppIcons.ClientsFilled, { AdminClientsScreen() }),
    TECHNICIANS(2, "Técnicos", AppIcons.Technicians, AppIcons.TechniciansFilled, { AdminTechniciansScreen() }),
    ORDERS(3, "Órdenes", AppIcons.Orders, AppIcons.OrdersFilled, { AdminOrdersScreen() }),
    QUOTES(4, "Cotizaciones", AppIcons.Quotes, AppIcons.QuotesFilled, { AdminQuotesScreen() }),
    NOTIFICATIONS(5, "Notificaciones", AppIcons.Notifications, AppIcons.NotificationsFilled, { NotificationsScreen() }),
}

private fun tabIndexFor(screen: Screen): Int = when (screen) {
    is AdminClientsScreen, is AdminClientDetailScreen, is AdminClientNewScreen -> 1
    is AdminTechniciansScreen, is AdminCreateTechnicianScreen -> 2
    is AdminOrdersScreen -> 3
    is AdminQuotesScreen -> 4
    is NotificationsScreen -> 5
    else -> 0
}
