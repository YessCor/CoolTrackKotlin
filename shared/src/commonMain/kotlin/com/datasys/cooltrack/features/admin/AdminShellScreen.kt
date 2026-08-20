package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.ui.components.AppIcons

/**
 * Equivalente a `AdminLayout` (`admin_layout.dart`) + el `ShellRoute` de
 * admin en `core/router.dart`.
 *
 * go_router usaba un `ShellRoute` con un único Navigator anidado
 * compartido por las 9 rutas `/admin/*` (dashboard, clients, technicians,
 * orders, quotes, equipment, catalog, tracking, reports) — la barra
 * inferior solo resalta 5 de esas 9 (`_calculateSelectedIndex` cae a 0
 * para equipment/catalog/tracking/reports, igual que acá). Se replica con
 * un único `Navigator` de Voyager: tocar un ítem de la barra reemplaza
 * todo el stack (`replaceAll`, equivalente a `context.go(path)`), mientras
 * que las pantallas internas (detalle de cliente, nuevo equipo, etc.)
 * usan `navigator.push(...)` sobre ese mismo Navigator (equivalente a las
 * sub-rutas anidadas de cada `GoRoute`).
 */
class AdminShellScreen : Screen {
    @Composable
    override fun Content() {
        Navigator(AdminDashboardScreen()) { navigator ->
            val current = navigator.lastItem
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        AdminTab.entries.forEach { tab ->
                            val selected = tabIndexFor(current) == tab.index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { if (!selected) navigator.replaceAll(tab.screen()) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.filledIcon else tab.icon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppColors.Secondary,
                                    selectedTextColor = AppColors.Secondary,
                                    unselectedIconColor = AppColors.TextMuted,
                                    unselectedTextColor = AppColors.TextMuted,
                                    indicatorColor = AppColors.Secondary.copy(alpha = 0.12f),
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    CurrentScreen()
                }
            }
        }
    }
}

/** Equivalente a los 5 `BottomNavigationBarItem` de `admin_layout.dart`. */
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
}

/** Equivalente a `_calculateSelectedIndex` en `admin_layout.dart`. */
private fun tabIndexFor(screen: Screen): Int = when (screen) {
    is AdminClientsScreen, is AdminClientDetailScreen, is AdminClientNewScreen -> 1
    is AdminTechniciansScreen, is AdminCreateTechnicianScreen -> 2
    is AdminOrdersScreen -> 3
    is AdminQuotesScreen -> 4
    // Dashboard, Equipment, Catalog, Tracking, Reports: sin caso explícito
    // en el original tampoco -> cae a "Dashboard" (índice 0).
    else -> 0
}
