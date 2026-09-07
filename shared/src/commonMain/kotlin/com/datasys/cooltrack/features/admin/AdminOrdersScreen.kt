package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.ui.components.AppAsyncContent
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppLeadingIcon
import com.datasys.cooltrack.ui.components.AppScreenScaffold
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.Spacing
import com.datasys.cooltrack.ui.components.staggeredItem
import org.koin.compose.koinInject

/**
 * Equivalente a admin_orders_screen.dart (incluye su `ordersProvider`
 * local, ahora vía `AdminRepository.getAllOrders()` sobre Supabase).
 */
class AdminOrdersScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val adminRepository: AdminRepository = koinInject()
        var orders by remember { mutableStateOf<List<ServiceOrder>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        suspend fun load() {
            error = null
            try {
                orders = adminRepository.getAllOrders()
            } catch (e: Exception) {
                error = e.message ?: "No se pudieron cargar las órdenes"
            }
        }

        LaunchedEffect(Unit) { load() }

        AppScreenScaffold(
            title = "Órdenes de Servicio",
            actions = {
                IconButton(onClick = { }) {
                    Icon(AppIcons.Filter, contentDescription = "Filtrar")
                }
            },
        ) { padding ->
            AppAsyncContent(
                data = orders,
                error = error,
                emptyIcon = AppIcons.Orders,
                emptyTitle = "No hay órdenes",
                emptyMessage = "Todavía no se registraron órdenes de servicio.",
                modifier = Modifier.padding(padding),
            ) { list ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Spacing.screen,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(list) { index, order ->
                        val statusColor = statusColor(order.status)
                        AppCard(
                            modifier = Modifier.staggeredItem(index),
                            onTap = { navigator.push(AdminOrderDetailScreen(order.id)) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppLeadingIcon(AppIcons.Orders, tint = statusColor)
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text("Orden #${order.orderNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(2.dp))
                                    Text(order.serviceType, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                    Text(
                                        order.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = AppColors.TextMuted,
                                    )
                                }
                            }
                            Spacer(Modifier.height(Spacing.md))
                            AppStatusBadge(order.status)
                        }
                    }
                }
            }
        }
    }
}

/** Equivalente a `_getStatusColor` en admin_orders_screen.dart. */
internal fun statusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.PENDING -> AppColors.StatusPending
    OrderStatus.ASSIGNED -> AppColors.StatusAssigned
    OrderStatus.ACCEPTED -> AppColors.StatusAccepted
    OrderStatus.IN_TRANSIT -> AppColors.StatusInTransit
    OrderStatus.IN_PROGRESS -> AppColors.StatusInProgress
    OrderStatus.COMPLETED -> AppColors.StatusCompleted
    OrderStatus.CANCELLED -> AppColors.StatusCancelled
}
