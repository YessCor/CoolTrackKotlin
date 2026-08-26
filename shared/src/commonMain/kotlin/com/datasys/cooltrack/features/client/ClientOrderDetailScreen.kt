package com.datasys.cooltrack.features.client

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.Quote
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.core.QuoteStatus
import com.datasys.cooltrack.ui.components.AppTopBar
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppButtonVariant
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Detalle de orden para el cliente (Módulo 5d).
 * Muestra estado, timeline, información del técnico y permite calificar.
 */
data class ClientOrderDetailScreen(val orderId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clientRepository: ClientRepository = koinInject()
        val toastState = rememberAppToastState()
        val scope = rememberCoroutineScope()

        var order by remember { mutableStateOf<ServiceOrder?>(null) }
        var quotes by remember { mutableStateOf<List<Quote>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var rating by remember { mutableIntStateOf(0) }
        var feedback by remember { mutableStateOf("") }
        var isRating by remember { mutableStateOf(false) }

        LaunchedEffect(orderId) {
            order = clientRepository.getOrderDetail(orderId)
            quotes = clientRepository.getQuotesForOrder(orderId)
            isLoading = false
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    expandedHeight = 44.dp,
                    title = { Text("Detalle de Orden") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(AppIcons.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                )
            },
            snackbarHost = { AppToastHost(toastState) },
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
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Header con número de orden y estado
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "Orden #${currentOrder.orderNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        currentOrder.createdAt.toString().split("T").first(),
                                        fontSize = 13.sp,
                                        color = AppColors.TextMuted,
                                    )
                                }
                                AppStatusBadge(currentOrder.status, large = true)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Información del servicio
                        AppCard {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Servicio", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(currentOrder.serviceType, fontWeight = FontWeight.Medium)

                                if (currentOrder.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Descripción", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(currentOrder.description)
                                }
                            }
                        }

                        // Cotizaciones
                        if (quotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Cotizaciones", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            quotes.forEach { quote ->
                                QuoteCard(
                                    quote = quote,
                                    onAccept = {
                                        scope.launch {
                                            clientRepository.updateQuoteStatus(quote.id, QuoteStatus.APPROVED)
                                            quotes = clientRepository.getQuotesForOrder(orderId)
                                            toastState.showSuccess("Cotización aceptada")
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            clientRepository.updateQuoteStatus(quote.id, QuoteStatus.REJECTED)
                                            quotes = clientRepository.getQuotesForOrder(orderId)
                                            toastState.showSuccess("Cotización rechazada")
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dirección
                        if (currentOrder.address.isNotBlank()) {
                            AppCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        AppIcons.Location,
                                        contentDescription = null,
                                        tint = AppColors.Secondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(currentOrder.address)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Timeline de estados
                        AppCard {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Progreso", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                                Spacer(modifier = Modifier.height(12.dp))
                                StatusTimeline(currentOrder)
                            }
                        }

                        // Sección de calificación (solo si está completada y no calificada)
                        if (currentOrder.isCompleted && currentOrder.clientRating == null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AppCard {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Calificar servicio", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Estrellas
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        for (i in 1..5) {
                                            IconButton(
                                                onClick = { rating = i },
                                                modifier = Modifier.size(36.dp),
                                            ) {
                                                Icon(
                                                    imageVector = if (i <= rating) AppIcons.Star else AppIcons.Star,
                                                    contentDescription = "$i estrellas",
                                                    tint = if (i <= rating) AppColors.Warning else AppColors.TextMuted,
                                                    modifier = Modifier.size(28.dp),
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = feedback,
                                        onValueChange = { feedback = it },
                                        label = { Text("Comentarios (opcional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    AppButton(
                                        label = if (isRating) "Enviando..." else "Enviar calificación",
                                        onPressed = {
                                            if (rating == 0) {
                                                scope.launch { toastState.showError("Selecciona una calificación") }
                                                return@AppButton
                                            }
                                            scope.launch {
                                                isRating = true
                                                try {
                                                    clientRepository.rateOrder(
                                                        orderId = currentOrder.id,
                                                        rating = rating,
                                                        feedback = feedback.ifBlank { null },
                                                    )
                                                    order = clientRepository.getOrderDetail(orderId)
                                                    toastState.showSuccess("Calificación enviada")
                                                } catch (e: Exception) {
                                                    toastState.showError("Error: ${e.message}")
                                                } finally {
                                                    isRating = false
                                                }
                                            }
                                        },
                                        isLoading = isRating,
                                        isFullWidth = true,
                                    )
                                }
                            }
                        }

                        // Mostrar calificación existente
                        if (currentOrder.clientRating != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AppCard {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Tu calificación", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row {
                                        for (i in 1..5) {
                                            Icon(
                                                imageVector = AppIcons.Star,
                                                contentDescription = null,
                                                tint = if (i <= (currentOrder.clientRating ?: 0)) AppColors.Warning else AppColors.TextMuted,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    if (!currentOrder.clientFeedback.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(currentOrder.clientFeedback!!, color = AppColors.TextSecondary)
                                    }
                                }
                            }
                        }

                        // Monto total
                        if (currentOrder.totalAmount != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AppCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Total", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "$" + String.format("%.2f", currentOrder.totalAmount),
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary,
                                        fontSize = 18.sp,
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

@Composable
private fun QuoteCard(quote: Quote, onAccept: () -> Unit, onReject: () -> Unit) {
    AppCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cotización #${quote.quoteNumber}", fontWeight = FontWeight.Bold)
                com.datasys.cooltrack.ui.components.AppQuoteStatusBadge(quote.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Total: ${quote.formattedTotal}", fontWeight = FontWeight.SemiBold, color = AppColors.Primary)
            
            if (quote.status == QuoteStatus.SENT) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(label = "Aceptar", onPressed = onAccept, modifier = Modifier.weight(1f))
                    AppButton(label = "Rechazar", onPressed = onReject, variant = AppButtonVariant.OUTLINE, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatusTimeline(order: ServiceOrder) {
    val steps = listOf(
        "Pendiente" to order.isPending,
        "Asignado" to order.isAssigned,
        "En progreso" to order.isInProgress,
        "Completado" to order.isCompleted,
    )
    val cancelled = order.status == com.datasys.cooltrack.core.OrderStatus.CANCELLED

    Column {
        steps.forEachIndexed { index, (label, isCurrent) ->
            val isPast = steps.drop(index + 1).any { it.second } ||
                    (index < steps.lastIndex && steps.drop(index + 1).all { !it.second }.not().not() &&
                            steps.drop(index + 1).any { (steps.indexOf(it) < index).not() })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when {
                                isCurrent -> AppColors.Secondary
                                isPast -> AppColors.Success
                                else -> AppColors.SurfaceBorder
                            },
                            shape = RoundedCornerShape(6.dp),
                        ),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isCurrent -> AppColors.TextPrimary
                        isPast -> AppColors.TextSecondary
                        else -> AppColors.TextMuted
                    },
                )
            }
            if (index < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(
                            if (steps.drop(index + 1).any { it.second }) AppColors.Success else AppColors.SurfaceBorder
                        ),
                )
            }
        }

        if (cancelled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Orden cancelada",
                color = AppColors.Error,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}
