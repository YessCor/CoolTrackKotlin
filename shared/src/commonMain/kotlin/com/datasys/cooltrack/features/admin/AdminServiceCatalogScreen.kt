package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.ServiceCatalog
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppLeadingIcon
import com.datasys.cooltrack.ui.components.AppListScreen
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.appEnter
import com.datasys.cooltrack.ui.components.formatCurrency
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Equivalente a admin_service_catalog_screen.dart. */
class AdminServiceCatalogScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val adminRepository: AdminRepository = koinInject()
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()

        var catalog by remember { mutableStateOf<List<ServiceCatalog>?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var editingItem by remember { mutableStateOf<ServiceCatalog?>(null) }

        suspend fun load() {
            errorMessage = null
            try {
                catalog = adminRepository.getServiceCatalog()
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }

        LaunchedEffect(Unit) { load() }

        AppListScreen(
            title = "Catálogo de Servicios",
            data = catalog,
            error = errorMessage,
            onBack = if (navigator.canPop) ({ navigator.pop() }) else null,
            emptyIcon = AppIcons.Catalog,
            emptyTitle = "Catálogo vacío",
            emptyMessage = "Aún no hay servicios cargados en el catálogo.",
            snackbarHost = { AppToastHost(toastState) },
        ) { list ->
            itemsIndexed(list) { index, item ->
                AppCard(
                    modifier = Modifier.appEnter(index),
                    onTap = { editingItem = item },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppLeadingIcon(AppIcons.Catalog, tint = AppColors.Secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                item.description ?: "Sin descripción",
                                color = AppColors.TextMuted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatCurrency(item.basePrice),
                                color = AppColors.Secondary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(item.unit, style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted)
                        }
                    }
                }
            }
        }

        val current = editingItem
        if (current != null) {
            var priceText by remember(current.id) { mutableStateOf(current.basePrice.toString()) }
            AlertDialog(
                onDismissRequest = { editingItem = null },
                title = { Text("Editar Precio: ${current.name}") },
                text = {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Precio Base") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val newPrice = priceText.toDoubleOrNull()
                        if (newPrice != null) {
                            scope.launch {
                                try {
                                    adminRepository.updateServicePrice(current.id, newPrice)
                                    editingItem = null
                                    load()
                                } catch (e: Exception) {
                                    toastState.showError("Error: ${e.message}")
                                }
                            }
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { editingItem = null }) { Text("Cancelar") }
                },
            )
        }
    }
}
