package com.datasys.cooltrack.features.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.ui.components.AppAsyncContent
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppLeadingIcon
import com.datasys.cooltrack.ui.components.AppScreenScaffold
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.Spacing
import com.datasys.cooltrack.ui.components.formatCurrency
import com.datasys.cooltrack.ui.components.formatShortDate
import com.datasys.cooltrack.ui.components.staggeredItem
import com.datasys.cooltrack.util.collectAsStateSimple
import org.koin.compose.koinInject

/**
 * Historial de órdenes para el cliente (Módulo 5d).
 */
class ClientOrdersScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clientRepository: ClientRepository = koinInject()
        val authRepository: AuthRepository = koinInject()

        val user = authRepository.state.collectAsStateSimple().value.user
        var orders by remember { mutableStateOf<List<ServiceOrder>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        suspend fun load() {
            error = null
            try {
                user?.id?.let { orders = clientRepository.getMyOrders(it) }
            } catch (e: Exception) {
                error = e.message ?: "No se pudieron cargar tus servicios"
            }
        }

        LaunchedEffect(user?.id) { load() }

        AppScreenScaffold(title = "Mis Servicios") { padding ->
            AppAsyncContent(
                data = orders,
                error = error,
                emptyIcon = AppIcons.Orders,
                emptyTitle = "Aún no tienes servicios",
                emptyMessage = "Cuando solicites un servicio, vas a poder seguirlo desde acá.",
                modifier = Modifier.padding(padding),
            ) { list ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Spacing.screen,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(list) { index, order ->
                        AppCard(
                            modifier = Modifier.staggeredItem(index),
                            onTap = { navigator.push(ClientOrderDetailScreen(order.id)) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppLeadingIcon(AppIcons.Orders, tint = AppColors.forOrderStatus(order.status))
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text("Orden #${order.orderNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(2.dp))
                                    Text(order.serviceType, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                }
                                AppStatusBadge(order.status)
                            }
                            Spacer(Modifier.height(Spacing.md))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    formatShortDate(order.createdAt.toString()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextMuted,
                                )
                                order.totalAmount?.let {
                                    Text(
                                        formatCurrency(it),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
