package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.datasys.cooltrack.ui.components.AppAsyncContent
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppLeadingIcon
import com.datasys.cooltrack.ui.components.AppQuoteStatusBadge
import com.datasys.cooltrack.ui.components.AppScreenScaffold
import com.datasys.cooltrack.ui.components.Spacing
import com.datasys.cooltrack.ui.components.staggeredItem
import org.koin.compose.koinInject

/**
 * Equivalente a admin_quotes_screen.dart (incluye su `quotesProvider`
 * local, ahora vía `AdminRepository.getAllQuotes()` sobre Supabase).
 *
 * La creación de cotizaciones ya no vive acá: el FAB que abría
 * `AdminQuoteNewScreen` se movió al menú del botón "+" del shell.
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

        AppScreenScaffold(title = "Cotizaciones") { padding ->
            AppAsyncContent(
                data = quotes,
                error = error,
                emptyIcon = AppIcons.Quotes,
                emptyTitle = "No hay cotizaciones",
                emptyMessage = "Creá una cotización desde el botón + para enviarla a un cliente.",
                modifier = Modifier.padding(padding),
            ) { list ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Spacing.screen,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(list) { index, quote ->
                        AppCard(
                            modifier = Modifier.staggeredItem(index),
                            onTap = { navigator.push(AdminQuoteDetailScreen(quote.id)) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppLeadingIcon(AppIcons.Quotes, tint = quoteStatusColor(quote.status))
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text("Cotización #${quote.quoteNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(2.dp))
                                    Text(quote.formattedTotal, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                }
                            }
                            Spacer(Modifier.height(Spacing.md))
                            AppQuoteStatusBadge(quote.status)
                        }
                    }
                }
            }
        }
    }
}

/** Equivalente a `_getStatusColor` en admin_quotes_screen.dart. */
internal fun quoteStatusColor(status: QuoteStatus): Color = when (status) {
    QuoteStatus.DRAFT -> AppColors.TextMuted
    QuoteStatus.SENT -> AppColors.Info
    QuoteStatus.APPROVED -> AppColors.Success
    QuoteStatus.REJECTED -> AppColors.Error
    QuoteStatus.EXPIRED -> AppColors.Warning
}
