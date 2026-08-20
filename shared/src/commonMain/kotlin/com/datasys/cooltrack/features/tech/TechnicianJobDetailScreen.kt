package com.datasys.cooltrack.features.tech

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.core.technicianNextStatus
import com.datasys.cooltrack.features.admin.AdminRepository
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.ui.components.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Detalle del trabajo para el técnico.
 * Permite cambiar estados (Aceptar -> En camino -> En progreso -> Finalizado).
 */
data class TechnicianJobDetailScreen(val orderId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val techRepository: TechRepository = koinInject()
        val adminRepository: AdminRepository = koinInject() // Para getOrderDetail
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()

        var order by remember { mutableStateOf<ServiceOrder?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(orderId) {
            order = adminRepository.getOrderDetail(orderId)
            isLoading = false
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalle de Orden") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(AppIcons.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                )
            },
            snackbarHost = { AppToastHost(toastState) }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                order?.let { currentOrder ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AppCard {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Orden #${currentOrder.orderNumber}", fontWeight = FontWeight.Bold)
                                    AppStatusBadge(currentOrder.status)
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Servicio: ${currentOrder.serviceType}", style = MaterialTheme.typography.bodyMedium)
                                Text("Descripción: ${currentOrder.description}", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Ubicación", fontWeight = FontWeight.SemiBold)
                        AppCard {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(currentOrder.address)
                                // Aquí se podría agregar un botón para abrir Waze/Google Maps
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Flujo de estados del técnico
                        val nextStatus = technicianNextStatus[currentOrder.status]
                        if (nextStatus != null) {
                            AppButton(
                                text = buttonLabelFor(nextStatus),
                                onClick = {
                                    scope.launch {
                                        try {
                                            techRepository.updateJobStatus(currentOrder.id, nextStatus)
                                            order = adminRepository.getOrderDetail(orderId) // Recargar
                                            toastState.showSuccess("Estado actualizado a ${nextStatus.label}")
                                        } catch (e: Exception) {
                                            toastState.showError("Error al actualizar: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (currentOrder.status == OrderStatus.COMPLETED) {
                            Text(
                                "Trabajo Finalizado",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = AppColors.Success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buttonLabelFor(status: OrderStatus): String = when (status) {
        OrderStatus.ACCEPTED -> "Aceptar Trabajo"
        OrderStatus.IN_TRANSIT -> "Iniciar Viaje"
        OrderStatus.IN_PROGRESS -> "Iniciar Trabajo"
        OrderStatus.COMPLETED -> "Finalizar Trabajo"
        else -> status.label
    }
}
