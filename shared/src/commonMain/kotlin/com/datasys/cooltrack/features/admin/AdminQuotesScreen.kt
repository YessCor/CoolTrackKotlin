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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.ApiClient
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.QuoteStatus
import com.datasys.cooltrack.core.getListData
import com.datasys.cooltrack.models.Quote
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons

/**
 * Equivalente a admin_quotes_screen.dart (incluye su `quotesProvider`
 * local, vía `ApiClient.get('/quotes')`). Igual que en el original, los
 * ítems de la lista no navegan a ningún detalle (no existe una
 * `admin_quote_detail_screen.dart`) — el FAB sí se conectó a
 * `AdminQuoteNewScreen`, ya que en Dart quedaba como comentario
 * `// New quote` sin implementar y la pantalla de creación sí existe.
 */
class AdminQuotesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var quotes by remember { mutableStateOf<List<Quote>?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                quotes = ApiClient.getListData("/quotes")
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Cotizaciones") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { navigator.push(AdminQuoteNewScreen()) }) {
                    Icon(imageVector = AppIcons.Add, contentDescription = "Nueva cotización")
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                val list = quotes
                when {
                    list == null && errorMessage == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: $errorMessage")
                    }
                    list != null && list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay cotizaciones")
                    }
                    list != null -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(list) { quote ->
                            val statusColor = quoteStatusColor(quote.status)
                            AppCard(
                                onTap = { navigator.push(AdminQuoteDetailScreen(quote.id)) },
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(AppColors.Secondary.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("#${quote.quoteNumber}", color = AppColors.Secondary, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Cotización #${quote.quoteNumber}", fontWeight = FontWeight.SemiBold)
                                        Text(quote.formattedTotal, fontSize = 13.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(quote.statusLabel, color = statusColor, fontSize = 12.sp)
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

/** Equivalente a `_getStatusColor` en admin_quotes_screen.dart. */
internal fun quoteStatusColor(status: QuoteStatus): Color = when (status) {
    QuoteStatus.DRAFT -> AppColors.TextMuted
    QuoteStatus.SENT -> AppColors.Info
    QuoteStatus.APPROVED -> AppColors.Success
    QuoteStatus.REJECTED -> AppColors.Error
    QuoteStatus.EXPIRED -> AppColors.Warning
}
