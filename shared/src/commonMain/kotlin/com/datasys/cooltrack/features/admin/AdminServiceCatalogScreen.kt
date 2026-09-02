package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.ServiceCatalog
import com.datasys.cooltrack.ui.components.AppTopBar
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Equivalente a admin_service_catalog_screen.dart. */
class AdminServiceCatalogScreen : Screen {
    @Composable
    override fun Content() {
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

        Scaffold(
            topBar = { AppTopBar(
                    expandedHeight = 44.dp,title = { Text("Catálogo de Servicios") }) },
            snackbarHost = { AppToastHost(toastState) },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                val list = catalog
                when {
                    list == null && errorMessage == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: $errorMessage")
                    }
                    list != null && list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay servicios en el catálogo")
                    }
                    list != null -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(list) { item ->
                            AppCard(onTap = { editingItem = item }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold)
                                        Text(item.description ?: "Sin descripción", color = AppColors.TextMuted, fontSize = 13.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            formatPrice(item.basePrice),
                                            color = AppColors.Secondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                        )
                                        Text(item.unit, fontSize = 10.sp, color = AppColors.TextMuted)
                                    }
                                }
                            }
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

/** Mismo criterio de formateo simple que `Quote.formattedTotal` (ver models/Quote.kt). */
private fun formatPrice(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100
    return "$" + rounded.toString()
}
