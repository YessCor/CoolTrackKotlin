package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.QuoteStatus
import com.datasys.cooltrack.models.Quote
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppLeadingIcon
import com.datasys.cooltrack.ui.components.AppListScreen
import com.datasys.cooltrack.ui.components.AppQuoteStatusBadge
import com.datasys.cooltrack.ui.components.appEnter
import org.koin.compose.koinInject

/**
 * Lista de cotizaciones para admin. La creación vive en el menú "+".
 */
class AdminQuotesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val adminRepository: AdminRepository = koinInject()
        var quotes by remember { mutableStateOf<List<Quote>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        suspend fun load() {
            error = null
            try {
                quotes = adminRepository.getAllQuotes()
            } catch (e: Exception) {
                error = e.message ?: "No se pudieron cargar las cotizaciones"
            }
        }

        LaunchedEffect(Unit) { load() }

        AppListScreen(
            title = "Cotizaciones",
            data = quotes,
            error = error,
            onBack = if (navigator.canPop) ({ navigator.pop() }) else null,
            emptyIcon = AppIcons.Quotes,
            emptyTitle = "No hay cotizaciones",
            emptyMessage = "Creá una cotización desde el botón + para enviarla a un cliente.",
        ) { list ->
            itemsIndexed(list) { index, quote ->
                AppCard(
                    modifier = Modifier.appEnter(index),
                    onTap = { navigator.push(AdminQuoteDetailScreen(quote.id)) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLeadingIcon(AppIcons.Quotes, tint = quoteStatusColor(quote.status))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Cotización #${quote.quoteNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(2.dp))
                            Text(quote.formattedTotal, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    AppQuoteStatusBadge(quote.status)
                }
            }
        }
    }
}

internal fun quoteStatusColor(status: QuoteStatus): Color = when (status) {
    QuoteStatus.DRAFT -> AppColors.TextMuted
    QuoteStatus.SENT -> AppColors.Info
    QuoteStatus.APPROVED -> AppColors.Success
    QuoteStatus.REJECTED -> AppColors.Error
    QuoteStatus.EXPIRED -> AppColors.Warning
}
