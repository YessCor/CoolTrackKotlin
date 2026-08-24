package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.ServiceCatalog
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.models.User
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInput
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.days
import org.koin.compose.koinInject

/**
 * Fila mínima decodificada tras el insert de `quotes` (necesitamos el `id`
 * generado por Postgres para encadenar el insert de `quote_items`).
 *
 * Nota de riesgo: `.insert(value) { select(...) }` + `.decodeSingle<T>()`
 * es el patrón estándar de supabase-kt (equivalente a `.insert().select().single()`
 * del cliente JS) — si la versión de `postgrest-kt` resuelta por Gradle
 * difiere, este es el punto más probable a ajustar.
 */
@Serializable
private data class IdOnly(val id: String)

/**
 * Estado editable de una fila de ítem (equivalente a `_QuoteItemRowData`).
 * Los `TextEditingController` de Dart se reemplazan con `mutableStateOf`
 * simples — no hace falta `dispose()` explícito, Compose libera el estado
 * junto con el composable.
 */
private class QuoteItemRowState {
    var catalogItemId by mutableStateOf<String?>(null)
    var description by mutableStateOf("")
    var quantity by mutableStateOf("1")
    var unitPrice by mutableStateOf("0")

    val total: Double get() = (quantity.toDoubleOrNull() ?: 0.0) * (unitPrice.toDoubleOrNull() ?: 0.0)
}

/**
 * Equivalente a admin_quote_new_screen.dart. El insert directo a Supabase
 * (`quotes` + `quote_items`) se mantiene igual que el original (esta
 * pantalla no pasa por el backend REST como la mayoría de las de admin).
 */
@OptIn(ExperimentalMaterial3Api::class)
class AdminQuoteNewScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()
        val adminRepository: AdminRepository = koinInject()
        val supabase: SupabaseClient = koinInject()

        var clients by remember { mutableStateOf<List<User>?>(null) }
        var catalog by remember { mutableStateOf<List<ServiceCatalog>?>(null) }
        var orders by remember { mutableStateOf<List<ServiceOrder>?>(null) }

        var selectedClientId by remember { mutableStateOf<String?>(null) }
        var selectedOrderId by remember { mutableStateOf<String?>(null) }
        var clientError by remember { mutableStateOf<String?>(null) }
        var notes by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        val items = remember { SnapshotStateList<QuoteItemRowState>().apply { add(QuoteItemRowState()) } }

        LaunchedEffect(Unit) {
            clients = try { adminRepository.getActiveClients() } catch (e: Exception) { emptyList() }
            catalog = try { adminRepository.getServiceCatalog() } catch (e: Exception) { emptyList() }
            orders = try { adminRepository.getRecentOrders() } catch (e: Exception) { emptyList() }
        }

        val subtotal = items.sumOf { it.total }
        val tax = subtotal * 0.16
        val total = subtotal + tax

        fun createQuote() {
            clientError = if (selectedClientId == null) "Seleccione un cliente" else null
            val hasEmptyDescription = items.any { it.description.trim().isEmpty() }
            if (clientError != null || hasEmptyDescription) {
                if (hasEmptyDescription) toastState.showError("Complete la descripción de todos los items")
                return
            }

            scope.launch {
                isSaving = true
                try {
                    val validUntil = Clock.System.now() + 15.days
                    val quoteJson = buildJsonObject {
                        put("client_id", selectedClientId)
                        selectedOrderId?.let { put("order_id", it) }
                        put("status", "sent")
                        put("subtotal", subtotal)
                        put("tax_rate", 16.0)
                        put("tax_amount", tax)
                        put("total", total)
                        put("notes", notes.trim())
                        put("valid_until", validUntil.toString())
                    }

                    val quoteId = supabase.from("quotes")
                        .insert(quoteJson) { select(Columns.list("id")) }
                        .decodeSingle<IdOnly>()
                        .id

                    val itemsJson: List<JsonObject> = items.map { item ->
                        buildJsonObject {
                            put("quote_id", quoteId)
                            item.catalogItemId?.let { put("catalog_item_id", it) }
                            put("description", item.description)
                            put("quantity", item.quantity.toDoubleOrNull() ?: 1.0)
                            put("unit_price", item.unitPrice.toDoubleOrNull() ?: 0.0)
                            put("total", item.total)
                        }
                    }
                    supabase.from("quote_items").insert(itemsJson)

                    toastState.showSuccess("Cotización creada y enviada")
                    navigator.pop()
                } catch (e: Exception) {
                    toastState.showError("Error: ${e.message}")
                } finally {
                    isSaving = false
                }
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Nueva Cotización") }) },
            snackbarHost = { AppToastHost(toastState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // Información general
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Información General", fontWeight = FontWeight.Bold)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        val clientsList = clients
                        if (clientsList == null) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            var clientExpanded by remember { mutableStateOf(false) }
                            val selectedClientName = clientsList.firstOrNull { it.id == selectedClientId }?.name ?: ""
                            ExposedDropdownMenuBox(expanded = clientExpanded, onExpandedChange = { clientExpanded = it }) {
                                OutlinedTextField(
                                    value = selectedClientName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Cliente") },
                                    isError = clientError != null,
                                    supportingText = clientError?.let { { Text(it) } },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                )
                                ExposedDropdownMenu(expanded = clientExpanded, onDismissRequest = { clientExpanded = false }) {
                                    clientsList.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c.name) },
                                            onClick = { selectedClientId = c.id; clientError = null; clientExpanded = false },
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val ordersList = orders
                        if (ordersList == null) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            var orderExpanded by remember { mutableStateOf(false) }
                            val selectedOrderLabel = ordersList.firstOrNull { it.id == selectedOrderId }
                                ?.let { "Orden #${it.orderNumber}" } ?: "Ninguna"
                            ExposedDropdownMenuBox(expanded = orderExpanded, onExpandedChange = { orderExpanded = it }) {
                                OutlinedTextField(
                                    value = selectedOrderLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Vincular a Orden (Opcional)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                )
                                ExposedDropdownMenu(expanded = orderExpanded, onDismissRequest = { orderExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Ninguna") },
                                        onClick = { selectedOrderId = null; orderExpanded = false },
                                    )
                                    ordersList.forEach { o ->
                                        DropdownMenuItem(
                                            text = { Text("Orden #${o.orderNumber}") },
                                            onClick = { selectedOrderId = o.id; orderExpanded = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Items de la Cotización", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                val catalogList = catalog
                if (catalogList == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    items.forEachIndexed { index, item ->
                        QuoteItemRow(
                            item = item,
                            catalog = catalogList,
                            canRemove = items.size > 1,
                            onRemove = { items.removeAt(index) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    OutlinedButton(onClick = { items.add(QuoteItemRowState()) }) {
                        Icon(imageVector = AppIcons.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar otro item")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Totales
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.05f)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TotalRow("Subtotal", subtotal)
                        Spacer(modifier = Modifier.height(4.dp))
                        TotalRow("IVA (16%)", tax)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        TotalRow("TOTAL", total, isBold = true, color = AppColors.Secondary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Notas / Términos", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                AppInput(
                    value = notes,
                    onValueChange = { notes = it },
                    hint = "Ej: Válido por 15 días. Incluye materiales.",
                    maxLines = 3,
                )

                Spacer(modifier = Modifier.height(32.dp))
                AppButton(
                    label = "Generar Cotización",
                    onPressed = ::createQuote,
                    isLoading = isSaving,
                    isFullWidth = true,
                    height = 50.dp,
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteItemRow(
    item: QuoteItemRowState,
    catalog: List<ServiceCatalog>,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                var catalogExpanded by remember { mutableStateOf(false) }
                val selectedLabel = catalog.firstOrNull { it.id == item.catalogItemId }?.name ?: "Manual / Otro"
                ExposedDropdownMenuBox(
                    expanded = catalogExpanded,
                    onExpandedChange = { catalogExpanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Servicio del Catálogo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catalogExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = catalogExpanded, onDismissRequest = { catalogExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Manual / Otro") },
                            onClick = { item.catalogItemId = null; catalogExpanded = false },
                        )
                        catalog.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    item.catalogItemId = c.id
                                    item.description = c.name
                                    item.unitPrice = c.basePrice.toString()
                                    catalogExpanded = false
                                },
                            )
                        }
                    }
                }
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(imageVector = AppIcons.Delete, contentDescription = "Quitar item", tint = AppColors.Error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            AppInput(
                value = item.description,
                onValueChange = { item.description = it },
                label = "Descripción personalizada",
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    AppInput(
                        value = item.quantity,
                        onValueChange = { item.quantity = it },
                        label = "Cant.",
                        keyboardType = KeyboardType.Number,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(2f)) {
                    AppInput(
                        value = item.unitPrice,
                        onValueChange = { item.unitPrice = it },
                        label = "Precio Unit.",
                        keyboardType = KeyboardType.Decimal,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(2f), horizontalAlignment = Alignment.End) {
                    Text("Total Item", fontSize = 10.sp, color = AppColors.TextMuted)
                    Text(formatMoney(item.total), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: Double, isBold: Boolean = false, color: Color? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(
            formatMoney(value),
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color ?: Color.Unspecified,
            fontSize = if (isBold) 18.sp else 14.sp,
        )
    }
}

private fun formatMoney(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100
    return "$" + rounded.toString()
}
