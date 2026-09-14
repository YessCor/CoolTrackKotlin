package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.core.QuoteStatus
import com.datasys.cooltrack.models.Equipment
import com.datasys.cooltrack.models.Quote
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.models.User
import com.datasys.cooltrack.notifications.NotificationRepository
import com.datasys.cooltrack.services.PdfContentBuilder
import com.datasys.cooltrack.services.PdfService
import com.datasys.cooltrack.services.SyncService
import com.datasys.cooltrack.models.formatMoney
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppButtonVariant
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppErrorState
import com.datasys.cooltrack.ui.components.AppHeroScaffold
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppModal
import com.datasys.cooltrack.ui.components.AppQuoteStatusBadge
import com.datasys.cooltrack.ui.components.AppSkeletonListCard
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.compose.koinInject

/**
 * Equivalente a admin_order_detail_screen.dart. `techniciansProvider` acá
 * es `AdminRepository.getActiveTechnicians()` (Supabase-directo) — esta es
 * la única pantalla del proyecto original que sí termina usando esos
 * providers "por paridad" de `admin_provider.dart` (ver nota en
 * `AdminRepository.kt`).
 *
 * Nota fiel al original: el botón de PDF usa el usuario admin logueado
 * como si fuera el "cliente" del reporte (comentario propio del Dart:
 * *"En una implementación real, buscaríamos al usuario cliente por ID...
 * Por ahora usamos los datos de la orden"*) — es un atajo ya presente en
 * el código fuente, no algo agregado acá.
 */
class AdminOrderDetailScreen(private val orderId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()

        val adminRepository: AdminRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val notificationRepository: NotificationRepository = koinInject()
        val syncService: SyncService = koinInject()
        val pdfService: PdfService = koinInject()

        var order by remember { mutableStateOf<ServiceOrder?>(null) }
        var technicians by remember { mutableStateOf<List<User>>(emptyList()) }
        var quotes by remember { mutableStateOf<List<Quote>>(emptyList()) }
        var equipment by remember { mutableStateOf<Equipment?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isUpdating by remember { mutableStateOf(false) }
        var showAssignSheet by remember { mutableStateOf(false) }

        suspend fun loadOrder() {
            val fetchedOrder = adminRepository.getOrderDetail(orderId)
            order = fetchedOrder
            quotes = adminRepository.getQuotesForOrder(orderId)
            fetchedOrder?.equipmentId?.let {
                equipment = adminRepository.getEquipmentById(it)
            }
        }

        LaunchedEffect(orderId) {
            isLoading = true
            loadOrder()
            technicians = try {
                adminRepository.getActiveTechnicians()
            } catch (e: Exception) {
                emptyList()
            }
            isLoading = false
        }

        fun updateOrderStatus(newStatus: OrderStatus, technicianId: String? = null) {
            scope.launch {
                isUpdating = true
                try {
                    val updateData = buildJsonObject {
                        put("status", newStatus.value)
                        put("updated_at", Clock.System.now().toString())
                        technicianId?.let { put("technician_id", it) }
                    }
                    syncService.queueOrderUpdate(orderId, updateData)
                    syncService.queueHistoryLog(
                        orderId,
                        newStatus.value,
                        if (technicianId != null) "Técnico asignado" else "Estado actualizado por Admin",
                    )
                    scope.launch { syncService.syncAll() }

                    // Notificar al cliente (+ alerta si la prioridad es alta)
                    order?.clientId?.let { clientId ->
                        val highPriority = order?.priority != null && order!!.priority != "normal"
                        val title = if (technicianId != null) "Técnico Asignado" else "Actualización de Orden"
                        val message = if (technicianId != null) {
                            "Se ha asignado un técnico a tu orden #${order?.orderNumber}."
                        } else {
                            "Tu orden #${order?.orderNumber} ha cambiado a: ${newStatus.label}."
                        }
                        notificationRepository.sendNotification(
                            userId = clientId,
                            title = title,
                            message = message,
                            orderId = orderId,
                            type = if (highPriority) "alert" else "order"
                        )
                    }

                    // Notificar al técnico cuando se le asigna la orden
                    if (technicianId != null && technicianId != order?.technicianId) {
                        notificationRepository.sendNotification(
                            userId = technicianId,
                            title = "Nueva Orden Asignada",
                            message = "Se te ha asignado la orden #${order?.orderNumber}. Revísala en tu lista de trabajos.",
                            orderId = orderId,
                            type = "order"
                        )
                    }

                    toastState.showSuccess("Operación exitosa")
                    loadOrder()
                } catch (e: Exception) {
                    toastState.showError("Error: ${e.message}")
                } finally {
                    isUpdating = false
                }
            }
        }

        val order0 = order
        AppHeroScaffold(
            title = "Detalle de Orden",
            onBack = { navigator.pop() },
            heroContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Detalle de Orden", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Orden #${order0?.orderNumber ?: "…"}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                    }
                    if (order0 != null) {
                        IconButton(onClick = {
                            val client = authRepository.state.value.user
                            val content = PdfContentBuilder.forOrder(order0, client)
                            pdfService.previewOrderPdf(content, order0.orderNumber)
                        }) {
                            Icon(AppIcons.Document, contentDescription = "Exportar PDF", tint = Color.White)
                        }
                    }
                    if (isUpdating) {
                        Box(Modifier.size(20.dp)) { CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White) }
                    }
                }
                if (order0 != null) {
                    Spacer(Modifier.height(10.dp))
                    AppStatusBadge(order0.status, large = true)
                }
            },
            snackbarHost = { AppToastHost(toastState) },
        ) {
            val current = order
            when {
                isLoading && current == null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { AppSkeletonListCard() }
                }
                current == null -> AppErrorState(message = "No pudimos encontrar esta orden.", modifier = Modifier.fillMaxWidth())
                else -> Column {
                    // Asignación de técnico
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Técnico Asignado", fontWeight = FontWeight.Bold)
                            if (current.status == OrderStatus.PENDING || current.status == OrderStatus.ASSIGNED) {
                                TextButton(onClick = { showAssignSheet = true }) {
                                    Icon(imageVector = AppIcons.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cambiar")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(AppColors.SurfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(imageVector = AppIcons.Profile, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    current.technicianId?.let { "Técnico ID: ${it.take(8)}..." } ?: "Sin asignar",
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    if (current.technicianId != null) "Ya asignado" else "Esta orden requiere un técnico",
                                    color = AppColors.TextMuted,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Detalles del servicio
                    AppCard {
                        Text("Detalles del Servicio", fontWeight = FontWeight.Bold)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow("Servicio", current.serviceType)
                        DetailRow("Dirección", current.address)
                        DetailRow("Prioridad", current.priority)
                        DetailRow("Creado", formatDateTime(current.createdAt))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Descripción:", color = AppColors.TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(current.description)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Información del Equipo
                    equipment?.let { eq ->
                        AppCard {
                            Text("Información del Equipo", fontWeight = FontWeight.Bold)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            DetailRow("Nombre", eq.name)
                            DetailRow("Tipo", eq.typeLabel)
                            DetailRow("Marca/Modelo", "${eq.brand ?: ""} ${eq.model ?: ""}")
                            DetailRow("S/N", eq.serialNumber ?: "N/A")
                            eq.capacityTons?.let { DetailRow("Capacidad", "$it Ton") }
                            if (!eq.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Notas Técnicas del Equipo:", color = AppColors.TextMuted, fontSize = 12.sp)
                                Text(eq.notes!!, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Sección de Cotizaciones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cotizaciones", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        AppButton(
                            label = "Nueva",
                            onPressed = {
                                navigator.push(AdminQuoteNewScreen(initialClientId = current.clientId, initialOrderId = current.id))
                            },
                            variant = AppButtonVariant.TEXT,
                            height = 36.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (quotes.isEmpty()) {
                        Text(
                            "No hay cotizaciones para esta orden",
                            color = AppColors.TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        quotes.forEach { quote ->
                            AppCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                onTap = { navigator.push(AdminQuoteDetailScreen(quote.id)) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Cotización #${quote.quoteNumber}", fontWeight = FontWeight.Bold)
                                        Text(
                                            "Total: ${quote.formattedTotal}",
                                            color = AppColors.Primary,
                                            fontSize = 14.sp
                                        )
                                    }
                                    AppQuoteStatusBadge(quote.status)
                                }
                            }
                        }
                    }

                    // Monto / ingreso registrado: usa el total_amount de la orden (ahora se
                    // llena al aprobar la cotización) o, si todavía no se
                    // registró, el total de la cotización aprobada. Un
                    // total_amount en 0 se trata como "no registrado".
                    val orderTotal = (current.totalAmount?.takeIf { it > 0 })
                        ?: quotes.find { it.status == QuoteStatus.APPROVED }?.effectiveTotal
                    if (orderTotal != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    if (current.totalAmount != null) "Ingreso registrado" else "Total acordado",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                                Divider(modifier = Modifier.padding(vertical = 12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Total del trabajo", fontWeight = FontWeight.Medium)
                                    Text(
                                        formatMoney(orderTotal),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppColors.Primary,
                                        fontSize = 24.sp,
                                    )
                                }
                            }
                        }
                    }

                    if (current.status == OrderStatus.PENDING) {
                        Spacer(modifier = Modifier.height(24.dp))
                        AppButton(
                            label = "Cancelar Orden",
                            onPressed = { updateOrderStatus(OrderStatus.CANCELLED) },
                            icon = AppIcons.Cancel,
                            isFullWidth = true,
                            variant = AppButtonVariant.SECONDARY,
                        )
                    }
                }
            }
        }

        if (showAssignSheet) {
            AppModal(onDismissRequest = { showAssignSheet = false }, title = "Asignar Técnico") {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(technicians) { tech ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            onTap = {
                                showAssignSheet = false
                                updateOrderStatus(OrderStatus.ASSIGNED, technicianId = tech.id)
                            },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).background(AppColors.SurfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(imageVector = AppIcons.Profile, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(tech.name, fontWeight = FontWeight.Medium)
                                    Text(tech.email, color = AppColors.TextMuted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = AppColors.TextMuted)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

/** Equivalente a `_formatDate` (`'${d.day}/${d.month}/${d.year} ${d.hour}:${d.minute}'`). */
private fun formatDateTime(instant: Instant): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val minute = dt.minute.toString().padStart(2, '0')
    return "${dt.dayOfMonth}/${dt.monthNumber}/${dt.year} ${dt.hour}:$minute"
}
