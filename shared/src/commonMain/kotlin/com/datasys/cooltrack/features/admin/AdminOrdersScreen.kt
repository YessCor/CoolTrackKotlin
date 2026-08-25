package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
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
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                orders = adminRepository.getAllOrders()
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Órdenes de Servicio") },
                    actions = {
                        // El original deja el filtro sin implementar
                        // (comentario `// Filter`); se preserva igual acá.
                        IconButton(onClick = { }) {
                            Icon(imageVector = AppIcons.Filter, contentDescription = "Filtrar")
                        }
                    },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                val list = orders
                when {
                    list == null && errorMessage == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: $errorMessage")
                    }
                    list != null && list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay órdenes")
                    }
                    list != null -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(list) { order ->
                            val statusColor = statusColor(order.status)
                            AppCard(onTap = { navigator.push(AdminOrderDetailScreen(order.id)) }) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(statusColor.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(imageVector = AppIcons.Orders, contentDescription = null, tint = statusColor)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Orden ${order.orderNumber}", fontWeight = FontWeight.SemiBold)
                                        Text(order.serviceType, fontSize = 13.sp)
                                        Text(
                                            order.address,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = AppColors.TextMuted,
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(order.statusLabel, color = statusColor, fontSize = 12.sp)
                                    }
                                }
                            }
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
