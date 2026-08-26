package com.datasys.cooltrack.features.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.datasys.cooltrack.ui.components.AppTopBar
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppConfirmDialog
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.AppToastState
import com.datasys.cooltrack.ui.components.SyncIndicator
import com.datasys.cooltrack.ui.components.rememberAppToastState
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Equivalente a admin_dashboard_screen.dart, con una pasada de diseño
 * encima del original: header con gradiente + saludo, tarjetas de stats con
 * conteo animado y tinte de color por métrica, acciones rápidas con
 * feedback de presión, y aparición escalonada de la lista de órdenes —
 * mismo contrato de datos (`AdminRepository`), solo la capa visual cambia.
 */
class AdminDashboardScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
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

        Scaffold(
            topBar = {
                AppTopBar(
                    expandedHeight = 44.dp,
                    title = {
                        Text(
                            "Panel de Administración",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppColors.TextPrimary,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.Surface,
                        titleContentColor = AppColors.TextPrimary,
                        actionIconContentColor = AppColors.TextSecondary,
                    ),
                    actions = {
                        SyncIndicator()
                        RotatingRefreshButton(isLoading = isLoading) { scope.launch { refresh() } }
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
                                        .background(AppColors.Error, CircleShape),
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
            containerColor = AppColors.SurfaceVariant,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(20.dp))
                    GreetingHeader(
                        name = authState.user?.name,
                        activeOrders = stats?.activeOrders,
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionTitle("Resumen de Operaciones")
                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        isLoading && stats == null -> Box(
                            Modifier.fillMaxWidth().height(180.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = AppColors.Secondary) }

                        errorMessage != null && stats == null -> Text("Error: $errorMessage", color = AppColors.Error)

                        stats != null -> {
                            StatsGrid(stats!!)
                            Spacer(modifier = Modifier.height(12.dp))
                            SecondaryStatsRow(stats!!)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    SectionTitle("Acciones Rápidas")
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickActions(navigator, toastState)

                    Spacer(modifier = Modifier.height(28.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionTitle("Órdenes Recientes")
                        TextButton(onClick = { navigator.push(AdminOrdersScreen()) }) {
                            Text("Ver todas", color = AppColors.Secondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    when {
                        isLoading && recentOrders == null -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppColors.Secondary)
                        }
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
private fun SectionTitle(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
}

/** Ícono de refresco que gira mientras `isLoading` es true (en vez del ícono estático original). */
@Composable
private fun RotatingRefreshButton(isLoading: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "dashboard-refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "refresh-rotation",
    )
    IconButton(onClick = onClick) {
        Icon(
            imageVector = AppIcons.Refresh,
            contentDescription = "Recargar",
            modifier = Modifier.rotate(if (isLoading) rotation else 0f),
        )
    }
}

/**
 * Encabezado plano (sin degradado): avatar circular con inicial + saludo,
 * directamente sobre el fondo neutro de la pantalla — estilo "data-dense
 * dashboard" en vez de banner de marca a todo lo ancho.
 */
@Composable
private fun GreetingHeader(name: String?, activeOrders: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AppColors.Secondary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (name?.trim()?.firstOrNull() ?: 'A').uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.Secondary,
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "Hola${name?.let { ", ${it.trim().substringBefore(' ')}" } ?: ""}",
                style = MaterialTheme.typography.headlineSmall,
                color = AppColors.TextPrimary,
            )
            Text(
                text = if (activeOrders != null && activeOrders > 0) {
                    "Tenés $activeOrders ${if (activeOrders == 1) "orden activa" else "órdenes activas"} hoy"
                } else {
                    "Todo tranquilo por ahora, sin órdenes activas"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
            )
        }
    }
}

private data class StatSpec(
    val title: String,
    val value: Int,
    val icon: ImageVector,
    val color: Color,
)

/** Grilla 2x2 con tinte de color por card, ícono en badge circular y conteo animado. */
@Composable
private fun StatsGrid(stats: DashboardStats) {
    val items = listOf(
        StatSpec("Órdenes Totales", stats.totalOrders, AppIcons.Orders, AppColors.Secondary),
        StatSpec("Activas", stats.activeOrders, AppIcons.Technicians, AppColors.Warning),
        StatSpec("Completadas", stats.completedOrders, AppIcons.CheckFilled, AppColors.Success),
        StatSpec("Cotizaciones", stats.pendingQuotes, AppIcons.Quotes, AppColors.Info),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEachIndexed { colIndex, spec ->
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedStatCard(spec, rowIndex * 2 + colIndex)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = "Ingresos: ${stats.formattedRevenue}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AnimatedStatCard(spec: StatSpec, index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 },
    ) {
        val animatedValue = remember(spec.value) { Animatable(0f) }
        LaunchedEffect(spec.value) {
            delay(index * 70L)
            animatedValue.animateTo(spec.value.toFloat(), tween(700, easing = FastOutSlowInEasing))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
                .background(AppColors.Surface, RoundedCornerShape(20.dp))
                .border(1.dp, AppColors.SurfaceBorder, RoundedCornerShape(20.dp))
                .padding(14.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(spec.color.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(
                        text = animatedValue.value.toInt().toString(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                    )
                    Text(spec.title, fontSize = 12.sp, color = AppColors.TextSecondary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** Fila secundaria: cotizaciones pendientes + calificación promedio, antes ausentes de la UI. */
@Composable
private fun SecondaryStatsRow(stats: DashboardStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PillStat(
            modifier = Modifier.weight(1f),
            icon = AppIcons.Dollar,
            label = "Ingresos totales",
            value = stats.formattedRevenue,
            color = AppColors.Success,
        )
        PillStat(
            modifier = Modifier.weight(1f),
            icon = AppIcons.Star,
            label = "Calificación",
            value = if (stats.averageRating > 0) "${stats.formattedRating} ★" else "Sin datos",
            color = AppColors.Warning,
        )
    }
}

@Composable
private fun PillStat(modifier: Modifier = Modifier, icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = modifier
            .background(AppColors.Surface, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Text(label, fontSize = 11.sp, color = AppColors.TextMuted)
        }
    }
}

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onTap: (Navigator) -> Unit,
)

@Composable
private fun QuickActions(
    navigator: Navigator,
    toastState: AppToastState,
) {
    val actions = listOf(
        QuickAction(AppIcons.PersonAdd, "Cliente", AppColors.Secondary) { it.push(AdminClientNewScreen()) },
        QuickAction(AppIcons.Technicians, "Técnico", AppColors.StatusAccepted) { it.push(AdminCreateTechnicianScreen()) },
        QuickAction(AppIcons.Quotes, "Cotización", AppColors.Success) { it.push(AdminQuotesScreen()) },
        QuickAction(AppIcons.Catalog, "Catálogo", AppColors.Warning) { it.push(AdminServiceCatalogScreen()) },
        QuickAction(AppIcons.Map, "Rastreo", AppColors.Info) { it.push(AdminTechTrackingScreen()) },
        QuickAction(AppIcons.Reports, "Informes", AppColors.StatusInProgress) { it.push(AdminReportsScreen()) },
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(3).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowActions.forEach { action ->
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionTile(action, navigator)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(action: QuickAction, navigator: Navigator) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(120),
        label = "tile-scale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(tileScale)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(18.dp))
            .background(AppColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.SurfaceBorder, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null) { action.onTap(navigator) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(action.color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = action.icon, contentDescription = null, tint = action.color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(action.label, style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary)
    }
}

@Composable
private fun RecentOrdersList(orders: List<ServiceOrder>, navigator: Navigator) {
    if (orders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Surface, RoundedCornerShape(14.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center,
        ) { Text("No hay órdenes registradas", color = AppColors.TextMuted) }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        orders.forEachIndexed { index, order ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 60L)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 6 },
            ) {
                val statusColor = statusColor(order.status)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp))
                        .background(AppColors.Surface, RoundedCornerShape(14.dp))
                        .clickable { navigator.push(AdminOrdersScreen()) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(44.dp)
                            .background(statusColor, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                        Text("Orden #${order.orderNumber}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(order.serviceType, color = AppColors.TextMuted, fontSize = 12.sp)
                    }
                    AppStatusBadge(status = order.status)
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
    }
}
