package com.datasys.cooltrack.features.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.Equipment
import com.datasys.cooltrack.ui.components.*
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pantalla de lista de equipos para el cliente.
 */
class ClientEquipmentScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clientRepository: ClientRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val scope = rememberCoroutineScope()

        val user = authRepository.state.collectAsStateSimple().value.user
        var equipment by remember { mutableStateOf<List<Equipment>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        fun load() {
            user?.id?.let { id ->
                scope.launch {
                    error = null
                    equipment = null
                    try {
                        equipment = clientRepository.getMyEquipment(id)
                    } catch (e: Exception) {
                        error = e.message ?: "No se pudieron cargar tus equipos"
                    }
                }
            }
        }

        LaunchedEffect(user?.id) { load() }

        AppScreenScaffold(title = "Mis Equipos") { padding ->
            AppAsyncContent(
                data = equipment,
                error = error,
                emptyIcon = AppIcons.Equipment,
                emptyTitle = "Sin equipos registrados",
                emptyMessage = "Agregá tus equipos para que los técnicos sepan sobre qué van a trabajar.",
                emptyAction = {
                    AppButton(
                        label = "Agregar equipo",
                        icon = AppIcons.Add,
                        onPressed = { navigator.push(ClientEquipmentNewScreen()) },
                    )
                },
                onRetry = { load() },
                modifier = Modifier.padding(padding),
            ) { list ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Spacing.screen,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(list) { index, eq ->
                        AppCard(
                            modifier = Modifier.staggeredItem(index),
                            onTap = { navigator.push(ClientEquipmentNewScreen(eq)) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppLeadingIcon(AppIcons.Equipment, tint = AppColors.Secondary)
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text(eq.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        listOfNotNull(eq.brand, eq.model).joinToString(" ").ifBlank { "Sin marca" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary,
                                    )
                                    Text(
                                        "S/N: ${eq.serialNumber ?: "N/A"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextMuted,
                                    )
                                }
                                Icon(AppIcons.ChevronRight, contentDescription = null, tint = AppColors.TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
