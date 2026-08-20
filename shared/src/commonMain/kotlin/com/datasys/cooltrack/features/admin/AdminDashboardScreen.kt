package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppConfirmDialog
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.AppToastState
import com.datasys.cooltrack.ui.components.SyncIndicator
import com.datasys.cooltrack.ui.components.rememberAppToastState
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Equivalente a admin_dashboard_screen.dart. `statsAsync.when(...)` de
 * Riverpod se reemplaza por estado local (`stats`/`isLoadingStats`/`error`)
 * cargado en un `LaunchedEffect`, y el `RefreshIndicator` (swipe-to-refresh)
 * por un ícono de recarga en la AppBar — Compose Multiplatform todavía no
 * trae un `pull-to-refresh` estable en todas las plataformas de este
 * proyecto, así que se prefirió no apostar a una API que podría no existir
 * en la versión de Compose que resuelva Gradle.
 */
class AdminDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val adminRepository: AdminRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val notificationRepository: NotificationRepository = koinInject()
        val scope = rememberCoroutineScope()

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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Panel de Administración") },
                    actions = {
                        SyncIndicator()
                        IconButton(onClick = { scope.launch { refresh() } }) {
                            Icon(imageVector = AppIcons.Refresh, contentDescription = "Recargar")
                        }
                        Box {
                            IconButton(onClick = { navigator.push(NotificationsScreen()) }) {
                                Icon(imageVector = AppIcons.Notifications, contentDescription = "Notificaciones")
                            }
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 6.dp, end = 6.dp)
                                        .size(16.dp)
                                        .background(Color.Red, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(imageVector = AppIcons.Logout, contentDescription = "Cerrar sesión")
                        }
                    },
                )
            },
            snackbarHost = { AppToastHost(toastState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Text("Resumen de Operaciones", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading && stats == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    errorMessage != null && stats == null -> Text("Error: $errorMessage", color = AppColors.Error)
                    stats != null -> StatsGrid(stats!!)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Acciones Rápidas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                QuickActions(navigator, toastState)

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Órdenes Recientes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { navigator.push(AdminOrdersScreen()) }) { Text("Ver todas") }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    isLoading && recentOrders == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    recentOrders != null -> RecentOrdersList(recentOrders!!, navigator)
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
private fun StatsGrid(stats: DashboardStats) {
    val items = listOf(
        Triple("Órdenes Totales", stats.totalOrders.toString(), AppIcons.Orders) to AppColors.Secondary,
        Triple("Activas", stats.activeOrders.toString(), AppIcons.Technicians) to AppColors.Warning,
        Triple("Completadas", stats.completedOrders.toString(), AppIcons.CheckFilled) to AppColors.Success,
        Triple("Ingresos", stats.formattedRevenue, AppIcons.Dollar) to AppColors.Info,
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(180.dp),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items) { (data, color) ->
            val (title, value, icon) = data
            AppCard(modifier = Modifier.fillMaxWidth().aspectRatio(1.5f), padding = PaddingValues(12.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(title, fontSize = 11.sp, color = AppColors.TextSecondary)
                }
            }
        }
    }
}

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val onTap: (Navigator) -> Unit,
)

@Composable
private fun QuickActions(
    navigator: Navigator,
    toastState: AppToastState,
) {
    val actions = listOf(
        QuickAction(AppIcons.PersonAdd, "Cliente") { it.push(AdminClientNewScreen()) },
        QuickAction(AppIcons.Technicians, "Técnico") { it.push(AdminCreateTechnicianScreen()) },
        QuickAction(AppIcons.Quotes, "Cotización") { it.push(AdminQuotesScreen()) },
        QuickAction(AppIcons.Catalog, "Catálogo") { it.push(AdminServiceCatalogScreen()) },
        QuickAction(AppIcons.Map, "Rastreo") { it.push(AdminTechTrackingScreen()) },
        QuickAction(AppIcons.Reports, "Informes") { it.push(AdminReportsScreen()) },
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(180.dp),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(actions) { action ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(AppColors.SurfaceVariant, RoundedCornerShape(12.dp))
                    .clickable { action.onTap(navigator) },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(imageVector = action.icon, contentDescription = null, tint = AppColors.Primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(action.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecentOrdersList(orders: List<ServiceOrder>, navigator: Navigator) {
    if (orders.isEmpty()) {
        AppCard { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("No hay órdenes registradas") } }
        return
    }
    Column {
        orders.forEach { order ->
            AppCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                onTap = { navigator.push(AdminOrdersScreen()) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Orden #${order.orderNumber}", fontWeight = FontWeight.SemiBold)
                        Text(order.serviceType, color = AppColors.TextMuted, fontSize = 13.sp)
                    }
                    AppStatusBadge(status = order.status)
                }
            }
        }
    }
}
