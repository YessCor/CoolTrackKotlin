package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.features.notifications.NotificationsScreen
import com.datasys.cooltrack.models.DashboardStats
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.notifications.NotificationRepository
import com.datasys.cooltrack.ui.components.AppAnimatedCount
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppConfirmDialog
import com.datasys.cooltrack.ui.components.AppHeroAvatar
import com.datasys.cooltrack.ui.components.AppHeroHeader
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppLeadingIcon
import com.datasys.cooltrack.ui.components.AppSectionTitle
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.AppToastState
import com.datasys.cooltrack.ui.components.Spacing
import com.datasys.cooltrack.ui.components.SyncIndicator
import com.datasys.cooltrack.ui.components.appEnter
import com.datasys.cooltrack.ui.components.pressable
import com.datasys.cooltrack.ui.components.rememberAppToastState
import com.datasys.cooltrack.ui.components.spin
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject

/**
 * Dashboard de admin rediseñado como "command center": header heroico con
 * gradiente + saludo + KPIs vidriosos, y debajo una hoja con la grilla de
 * métricas (conteo animado), acciones rápidas y órdenes recientes — todo
 * con entrada escalonada.
 */
class AdminDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val adminRepository: AdminRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val notificationRepository: NotificationRepository = koinInject()
        val scope = rememberCoroutineScope()

        val authState by authRepository.state.collectAsStateSimple()
        val unreadCount by notificationRepository.unreadCount.collectAsStateSimple()

        var stats by remember { mutableStateOf<DashboardStats?>(null) }
        var recentOrders by remember { mutableStateOf<List<ServiceOrder>?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showLogoutDialog by remember { mutableStateOf(false) }
        val toastState = rememberAppToastState()

        suspend fun refresh() {
            isLoading = true
            errorMessage = null
            try {
                stats = adminRepository.getDashboardStats()
                recentOrders = adminRepository.getRecentOrders()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error desconocido"
            } finally {
                isLoading = false
            }
        }

        LaunchedEffect(Unit) {
            notificationRepository.start()
            refresh()
        }

        val firstName = authState.user?.name?.trim()?.substringBefore(' ')
        val active = stats?.activeOrders

        Box(Modifier.fillMaxSize().background(AppColors.Background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp),
            ) {
                AppHeroHeader(
                    bottomExtra = 52.dp,
                    actions = {
                        SyncIndicator()
                        IconButton(onClick = { scope.launch { refresh() } }) {
                            Icon(AppIcons.Refresh, contentDescription = "Recargar", tint = Color.White, modifier = Modifier.spin(isLoading))
                        }
                        Box {
                            IconButton(onClick = { navigator.push(NotificationsScreen()) }) {
                                Icon(AppIcons.Notifications, contentDescription = "Notificaciones", tint = Color.White)
                            }
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp)
                                        .size(16.dp).background(AppColors.Error, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(if (unreadCount > 9) "9+" else "$unreadCount", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        IconButton(onClick = { navigator.push(com.datasys.cooltrack.features.settings.AppSettingsScreen()) }) {
                            Icon(AppIcons.Settings, contentDescription = "Ajustes", tint = Color.White)
                        }
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(AppIcons.Logout, contentDescription = "Cerrar sesión", tint = Color.White)
                        }
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Hola${firstName?.let { ", $it" } ?: ""}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (active != null && active > 0)
                                    "Tenés $active ${if (active == 1) "orden activa" else "órdenes activas"} hoy"
                                else "Panel de administración",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                        }
                        AppHeroAvatar(initial = firstName ?: "A")
                    }
                    Spacer(Modifier.height(20.dp))
                    HeroKpiRow(stats)
                }

                Column(
                    modifier = Modifier
                        .offset(y = (-24).dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(AppColors.Background)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                ) {
                    AppSectionTitle("Resumen de operaciones", modifier = Modifier.appEnter(0))
                    Spacer(Modifier.height(12.dp))

                    when {
                        isLoading && stats == null -> LoadingBlock()
                        errorMessage != null && stats == null -> Text("Error: $errorMessage", color = AppColors.Error)
                        stats != null -> StatsGrid(stats!!)
                    }

                    Spacer(Modifier.height(28.dp))
                    AppSectionTitle("Acciones rápidas", modifier = Modifier.appEnter(1))
                    Spacer(Modifier.height(12.dp))
                    QuickActions(navigator, toastState)

                    Spacer(Modifier.height(28.dp))
                    AppSectionTitle(
                        "Órdenes recientes",
                        modifier = Modifier.appEnter(2),
                        trailing = {
                            TextButton(onClick = { navigator.push(AdminOrdersScreen()) }) {
                                Text("Ver todas", color = AppColors.Secondary, fontWeight = FontWeight.SemiBold)
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))

                    when {
                        isLoading && recentOrders == null -> LoadingBlock()
                        recentOrders != null -> RecentOrdersList(recentOrders!!, navigator)
                    }
                }
            }
        }

        if (showLogoutDialog) {
            AppConfirmDialog(
                onDismissRequest = { showLogoutDialog = false },
                onConfirm = { scope.launch { authRepository.logout() } },
                title = "Cerrar Sesión",
                message = "¿Estás seguro de que deseas salir?",
                confirmText = "Cerrar Sesión",
            )
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.Secondary)
    }
}

/** Fila de KPIs "vidriosos" dentro del header hero. */
@Composable
private fun HeroKpiRow(stats: DashboardStats?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HeroKpi("Activas", stats?.activeOrders, Modifier.weight(1f))
        HeroKpi("Completadas", stats?.completedOrders, Modifier.weight(1f))
        HeroKpi("Cotizaciones", stats?.pendingQuotes, Modifier.weight(1f))
    }
}

@Composable
private fun HeroKpi(label: String, value: Int?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.13f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 12.dp),
    ) {
        if (value != null) {
            AppAnimatedCount(
                value = value,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
        } else {
            Text("—", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
    }
}

private data class StatSpec(val title: String, val value: Int, val icon: ImageVector, val color: Color)

@Composable
private fun StatsGrid(stats: DashboardStats) {
    val items = listOf(
        StatSpec("Órdenes totales", stats.totalOrders, AppIcons.Orders, AppColors.Secondary),
        StatSpec("En curso", stats.activeOrders, AppIcons.Technicians, AppColors.Warning),
        StatSpec("Completadas", stats.completedOrders, AppIcons.CheckFilled, AppColors.Success),
        StatSpec("Ingresos", 0, AppIcons.Dollar, AppColors.StatusAccepted),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEachIndexed { rowIndex, rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEachIndexed { colIndex, spec ->
                    val i = rowIndex * 2 + colIndex
                    Box(Modifier.weight(1f).appEnter(i + 1)) {
                        if (spec.title == "Ingresos") {
                            RevenueCard(stats.formattedRevenue, spec)
                        } else {
                            StatCard(spec)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(spec: StatSpec) {
    AppCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Column(Modifier.fillMaxWidth().aspectRatio(1.35f), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(38.dp).background(spec.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(19.dp))
            }
            Column {
                AppAnimatedCount(value = spec.value, style = MaterialTheme.typography.displaySmall)
                Text(spec.title, style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun RevenueCard(revenue: String, spec: StatSpec) {
    AppCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Column(Modifier.fillMaxWidth().aspectRatio(1.35f), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(38.dp).background(spec.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(19.dp))
            }
            Column {
                Text(revenue, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary)
                Text("Ingresos totales", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
            }
        }
    }
}

private data class QuickAction(val icon: ImageVector, val label: String, val color: Color, val onTap: (Navigator) -> Unit)

@Composable
private fun QuickActions(navigator: Navigator, toastState: AppToastState) {
    val actions = listOf(
        QuickAction(AppIcons.PersonAdd, "Cliente", AppColors.Secondary) { it.push(AdminClientNewScreen()) },
        QuickAction(AppIcons.Technicians, "Técnico", AppColors.StatusAccepted) { it.push(AdminCreateTechnicianScreen()) },
        QuickAction(AppIcons.Quotes, "Cotización", AppColors.Success) { it.push(AdminQuotesScreen()) },
        QuickAction(AppIcons.Catalog, "Catálogo", AppColors.Warning) { it.push(AdminServiceCatalogScreen()) },
        QuickAction(AppIcons.Map, "Rastreo", AppColors.Accent) { it.push(AdminTechTrackingScreen()) },
        QuickAction(AppIcons.Reports, "Informes", AppColors.StatusInProgress) { it.push(AdminReportsScreen()) },
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(3).forEachIndexed { rowIndex, rowActions ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowActions.forEachIndexed { colIndex, action ->
                    Box(Modifier.weight(1f).appEnter(rowIndex * 3 + colIndex + 2)) {
                        QuickActionTile(action, navigator)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(action: QuickAction, navigator: Navigator) {
    AppCard(
        padding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        onTap = { action.onTap(navigator) },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(42.dp).background(action.color.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(action.icon, contentDescription = null, tint = action.color, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(action.label, style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary)
        }
    }
}

@Composable
private fun RecentOrdersList(orders: List<ServiceOrder>, navigator: Navigator) {
    if (orders.isEmpty()) {
        AppCard { Text("No hay órdenes registradas", color = AppColors.TextMuted, modifier = Modifier.padding(vertical = 12.dp)) }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        orders.forEachIndexed { index, order ->
            AppCard(
                modifier = Modifier.appEnter(index + 3),
                padding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                onTap = { navigator.push(AdminOrderDetailScreen(order.id)) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppLeadingIcon(AppIcons.Orders, tint = AppColors.forOrderStatus(order.status), size = 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Orden #${order.orderNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(order.serviceType, color = AppColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    AppStatusBadge(status = order.status)
                }
            }
        }
    }
}
