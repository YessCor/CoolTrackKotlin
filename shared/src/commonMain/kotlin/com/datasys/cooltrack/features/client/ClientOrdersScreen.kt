package com.datasys.cooltrack.features.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.datasys.cooltrack.ui.components.AppTopBar
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.AppCardSkeleton
import com.datasys.cooltrack.ui.components.AppEmptyState
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
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(user?.id) {
            user?.id?.let { id ->
                orders = clientRepository.getMyOrders(id)
                isLoading = false
            }
        }

        Scaffold(
            topBar = { AppTopBar(
                    expandedHeight = 44.dp,title = { Text("Mis Servicios") }) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigator.push(ClientRequestServiceScreen()) },
                    containerColor = AppColors.Secondary
                ) {
                    Icon(AppIcons.Add, contentDescription = "Solicitar Servicio")
                }
            }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val list = orders ?: emptyList()
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Aún no tienes solicitudes de servicio")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list) { order ->
                            AppCard(
                                onTap = { navigator.push(ClientOrderDetailScreen(order.id)) },
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Orden #${order.orderNumber}", fontWeight = FontWeight.Bold)
                                        AppStatusBadge(order.status)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(order.serviceType, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        order.createdAt.toString().split("T")[0],
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextMuted
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
