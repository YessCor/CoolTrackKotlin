package com.datasys.cooltrack.features.client

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.QuoteStatus
import com.datasys.cooltrack.models.Quote
import com.datasys.cooltrack.models.QuoteItem
import com.datasys.cooltrack.models.formatMoney
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppButtonVariant
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppErrorState
import com.datasys.cooltrack.ui.components.AppHeroScaffold
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppQuoteStatusBadge
import com.datasys.cooltrack.ui.components.AppSkeletonListCard
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Detalle de cotización para el cliente.
 * Muestra items, precios y permite Aceptar/Rechazar si está en estado SENT.
 */
data class ClientQuoteDetailScreen(val quoteId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clientRepository: ClientRepository = koinInject()
        val toastState = rememberAppToastState()
        val scope = rememberCoroutineScope()

        var quote by remember { mutableStateOf<Quote?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isUpdating by remember { mutableStateOf(false) }

        fun loadQuote() {
            scope.launch {
                isLoading = true
                quote = clientRepository.getQuoteById(quoteId)
                isLoading = false
            }
        }

        LaunchedEffect(quoteId) {
            loadQuote()
        }

        val q0 = quote
        AppHeroScaffold(
            title = "Detalle de Cotización",
            onBack = { navigator.pop() },
            snackbarHost = { AppToastHost(toastState) },
            heroContent = {
                Text("Cotización", style = MaterialTheme.typography.labelLarge, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))
                Text(
                    "#${q0?.quoteNumber ?: "…"}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                if (q0 != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppQuoteStatusBadge(q0.status, large = true)
                        Spacer(Modifier.width(10.dp))
                        Text(q0.formattedTotal, style = MaterialTheme.typography.titleSmall, color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            },
        ) {
            if (isLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { repeat(3) { AppSkeletonListCard() } }
            } else {
                quote?.let { q ->
                    Column {
                        // Items
                        if (!q.items.isNullOrEmpty()) {
                            AppCard {
                                Column {
                                    Text("Detalle de Items", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    q.items!!.forEach { item ->
                                        QuoteItemRow(item)
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Totales
                        AppCard {
                            Column {
                                SummaryRow("Subtotal", q.formattedSubtotal)
                                Spacer(modifier = Modifier.height(8.dp))
                                SummaryRow("IVA (${q.taxRatePercent.toInt()}%)", q.formattedTax)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                SummaryRow("TOTAL", q.formattedTotal, isBold = true)
                            }
                        }

                        // Notas
                        if (!q.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AppCard {
                                Column {
                                    Text("Notas adicionales", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(q.notes!!)
                                }
                            }
                        }

                        // Botones de acción
                        if (q.status == QuoteStatus.SENT) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                AppButton(
                                    label = "Rechazar",
                                    onPressed = {
                                        scope.launch {
                                            isUpdating = true
                                            try {
                                                clientRepository.updateQuoteStatus(q.id, QuoteStatus.REJECTED)
                                                toastState.showSuccess("Cotización rechazada")
                                                loadQuote()
                                            } catch (e: Exception) {
                                                toastState.showError("Error: ${e.message}")
                                            } finally {
                                                isUpdating = false
                                            }
                                        }
                                    },
                                    variant = AppButtonVariant.OUTLINE,
                                    modifier = Modifier.weight(1f),
                                    isLoading = isUpdating
                                )
                                AppButton(
                                    label = "Aceptar",
                                    onPressed = {
                                        scope.launch {
                                            isUpdating = true
                                            try {
                                                clientRepository.updateQuoteStatus(q.id, QuoteStatus.APPROVED)
                                                toastState.showSuccess("Cotización aceptada")
                                                loadQuote()
                                            } catch (e: Exception) {
                                                toastState.showError("Error: ${e.message}")
                                            } finally {
                                                isUpdating = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    isLoading = isUpdating
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                } ?: AppErrorState(
                    message = "No se pudo cargar la cotización.",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun QuoteItemRow(item: QuoteItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.description, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                "${item.quantity} × ${formatMoney(item.unitPrice)}",
                fontSize = 13.sp,
                color = AppColors.TextMuted,
            )
        }
        Text(
            formatMoney(item.total),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) AppColors.TextPrimary else AppColors.TextSecondary,
        )
        Text(
            value,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (isBold) 18.sp else 15.sp,
            color = if (isBold) AppColors.Primary else AppColors.TextPrimary,
        )
    }
}
