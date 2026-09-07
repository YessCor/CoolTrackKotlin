package com.datasys.cooltrack.features.admin

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.Quote
import com.datasys.cooltrack.models.QuoteItem
import com.datasys.cooltrack.models.formatMoney
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppErrorState
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppLoadingList
import com.datasys.cooltrack.ui.components.AppQuoteStatusBadge
import com.datasys.cooltrack.ui.components.AppScreenScaffold
import org.koin.compose.koinInject

/**
 * Detalle de cotización para admin.
 * Muestra items, subtotales, IVA y total.
 */
data class AdminQuoteDetailScreen(val quoteId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val adminRepository: AdminRepository = koinInject()
        var quote by remember { mutableStateOf<Quote?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(quoteId) {
            try {
                quote = adminRepository.getQuoteById(quoteId)
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }

        AppScreenScaffold(
            title = "Detalle de Cotización",
            onBack = { navigator.pop() },
        ) { padding ->
            when {
                isLoading -> AppLoadingList(modifier = Modifier.padding(padding), count = 4)
                errorMessage != null -> AppErrorState(
                    message = errorMessage!!,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
                quote == null -> AppErrorState(
                    message = "No pudimos encontrar esta cotización.",
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
                else -> {
                    val q = quote!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // Header
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "Cotización #${q.quoteNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        q.createdAt.toString().split("T").first(),
                                        fontSize = 13.sp,
                                        color = AppColors.TextMuted,
                                    )
                                }
                                AppQuoteStatusBadge(q.status, large = true)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Items
                        if (!q.items.isNullOrEmpty()) {
                            AppCard {
                                Column {
                                    Text("Items", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
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
                                SummaryRow("IVA (${(q.taxRate * 100).toInt()}%)", q.formattedTax)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                SummaryRow("Total", q.formattedTotal, isBold = true)
                            }
                        }

                        // Notas
                        if (!q.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AppCard {
                                Column {
                                    Text("Notas", fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(q.notes!!)
                                }
                            }
                        }

                        // Válido hasta
                        if (q.validUntil != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AppCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        AppIcons.Calendar,
                                        contentDescription = null,
                                        tint = AppColors.Secondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Válido hasta", fontSize = 13.sp, color = AppColors.TextSecondary)
                                        Text(
                                            q.validUntil!!.toString().split("T").first(),
                                            fontWeight = FontWeight.Medium,
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
