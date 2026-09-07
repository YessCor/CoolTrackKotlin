package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.Client
import com.datasys.cooltrack.ui.components.AppAsyncContent
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppScreenScaffold
import com.datasys.cooltrack.ui.components.AppTag
import com.datasys.cooltrack.ui.components.Spacing
import com.datasys.cooltrack.ui.components.staggeredItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Equivalente a admin_clients_screen.dart (incluye su `clientsProvider`
 * local, migrado acá directo como una llamada suspendida en
 * `LaunchedEffect` en vez de un `FutureProvider` aparte).
 */
class AdminClientsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val adminRepository: AdminRepository = koinInject()

        var clients by remember { mutableStateOf<List<Client>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        suspend fun load() {
            error = null
            clients = null
            try {
                clients = adminRepository.getAllClients()
            } catch (e: Exception) {
                error = e.message ?: "No se pudieron cargar los clientes"
            }
        }

        LaunchedEffect(Unit) { load() }

        AppScreenScaffold(title = "Clientes") { padding ->
            AppAsyncContent(
                data = clients,
                error = error,
                emptyIcon = AppIcons.Clients,
                emptyTitle = "No hay clientes",
                emptyMessage = "Registrá tu primer cliente para empezar a operar.",
                emptyAction = {
                    AppButton(
                        label = "Agregar cliente",
                        icon = AppIcons.PersonAdd,
                        onPressed = { navigator.push(AdminClientNewScreen()) },
                    )
                },
                onRetry = { scope.launch { load() } },
                modifier = Modifier.padding(padding),
            ) { list ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Spacing.screen,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(list) { index, client ->
                        AppCard(
                            modifier = Modifier.staggeredItem(index),
                            onTap = { navigator.push(AdminClientDetailScreen(client.id)) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(AppColors.Secondary.copy(alpha = 0.14f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        client.name.take(1).uppercase(),
                                        color = AppColors.Secondary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text(client.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(2.dp))
                                    Text(client.email, color = AppColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                if (!client.isActive) {
                                    AppTag("Inactivo", AppColors.Error)
                                    Spacer(Modifier.width(Spacing.sm))
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
